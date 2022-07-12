package com.netty.rpc.client.connect;

import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.client.handler.RpcClientInitializer;
import com.netty.rpc.client.route.RpcLoadBalance;
import com.netty.rpc.client.route.impl.RpcLoadBalanceRoundRobin;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *      RPC Connection Manager ：每次拿到最新zookeeper的数据，
 *
 *      服务发现类，每次发生节点变动，都会调用这个类，进同步节点信息
 *
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));
    //保存连接服务提供者的服务器的节点（也是缓存到本地）
    private Map<RpcProtocol, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    //rpcProtocolSet：客户端本地保存zookeeper上关于服务提供者的服务器的信息
    private CopyOnWriteArraySet<RpcProtocol> rpcProtocolSet = new CopyOnWriteArraySet<>();

    private ReentrantLock lock = new ReentrantLock();
    //condition的await，signal，signaAll需要配合lock和unlock使用
    private Condition connected = lock.newCondition();  //一个Lock里面可以创建多个Condition实例，实现多路通知

    private long waitTimeout = 5000;
    private RpcLoadBalance loadBalance = new RpcLoadBalanceRoundRobin();  //默认采用轮询策略实现负载均衡
    private volatile boolean isRunning = true;

    private ConnectionManager() {
    }

    private static class SingletonHolder {
        private static final ConnectionManager instance = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.instance;
    }

    /**
     *      如果zookeeper节点发生变动，那么相应的更新客户端这边关于服务器节点的信息
     * @param serviceList
     */
    public void updateConnectedServer(List<RpcProtocol> serviceList) {
        //现在使用两个集合来管理服务信息和TCP连接，因为建立连接是异步的
        //一旦服务信息在ZK上更新，将触发此函数
        //实际上客户端应该只关心它正在使用的服务
        if (serviceList != null && serviceList.size() > 0) {
            //更新本地服务器节点缓存
            HashSet<RpcProtocol> serviceSet = new HashSet<>(serviceList.size());
            for (int i = 0; i < serviceList.size(); ++i) {
                RpcProtocol rpcProtocol = serviceList.get(i);
                serviceSet.add(rpcProtocol);
            }

            // 添加新的服务器信息（rpc服务器）到本地缓存rpcProtocolSet
            for (final RpcProtocol rpcProtocol : serviceSet) {
                if (!rpcProtocolSet.contains(rpcProtocol)) {  //如果本地缓存没有包括这个服务器
                    //为什么要连接呢？难道我每次zookeeper更新之后，我都要把新的节点重连一次？为了确保客户端预服务之间进行长连接
                    //不需要每次调用服务的时候再进行长连接，通过监听zk服务节点的变化，动态更新客户端和服务端保持长连接
                    //但是我觉得不好，如果客户端数量过多，那么每个服务器都需要和大量的客户端保持连接，造成资源浪费
                    connectServerNode(rpcProtocol);
                }
            }

            // 关闭无效的节点信息，因为每次信息进行同步，都要保证缓存的是最新的一些节点
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                if (!serviceSet.contains(rpcProtocol)) {   //如果old的节点信息不包括新的节点信息，那么就把old节点去掉
                    logger.info("Remove invalid service: " + rpcProtocol.toJson());
                    removeAndCloseHandler(rpcProtocol);
                }
            }
        } else {
            // No available service
            logger.error("No available service!");
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                removeAndCloseHandler(rpcProtocol);
            }
        }
    }


    public void updateConnectedServer(RpcProtocol rpcProtocol, PathChildrenCacheEvent.Type type) {
        if (rpcProtocol == null) {
            return;
        }
        if (type == PathChildrenCacheEvent.Type.CHILD_ADDED && !rpcProtocolSet.contains(rpcProtocol)) {
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
            //TODO We may don't need to reconnect remote server if the server'IP and server'port are not changed
            removeAndCloseHandler(rpcProtocol);
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
            removeAndCloseHandler(rpcProtocol);
        } else {
            throw new IllegalArgumentException("Unknow type:" + type);
        }
    }

    /**
     * netty的服务端
     * 每次都要连接远程服务器，看看能不能连的通？？ 不是的，是为了建立长连接，
     * @param rpcProtocol
     */
    private void connectServerNode(RpcProtocol rpcProtocol) {
        if (rpcProtocol.getServiceInfoList() == null || rpcProtocol.getServiceInfoList().isEmpty()) {
            logger.info("No service on node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
            return;
        }
        rpcProtocolSet.add(rpcProtocol);
        logger.info("New service node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
        for (RpcServiceInfo serviceProtocol : rpcProtocol.getServiceInfoList()) {   //查看当前提供服务的服务器存在哪些服务
            logger.info("New service info, name: {}, version: {}", serviceProtocol.getServiceName(), serviceProtocol.getVersion());
        }
//        InetAddress:类的主要作用是封装IP及端口
        final InetSocketAddress remotePeer = new InetSocketAddress(rpcProtocol.getHost(), rpcProtocol.getPort());
        //使用线程池进行发送请求
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                //Netty 中 Bootstrap 类是客户端程序的启动引导类， ServerBootstrap 是服务端启动引导类
                //主要作用是配置整个 Netty 程序， 串联各个组件
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new RpcClientInitializer());

                ChannelFuture channelFuture = b.connect(remotePeer);  //连接到服务提供者的服务器
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {   //表示执行成功
                            logger.info("Successfully connect to remote server, remote peer = " + remotePeer);
//                            ChannelFuture的get()方法获取异步操作的结果
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            connectedServerNodes.put(rpcProtocol, handler);
                            handler.setRpcProtocol(rpcProtocol);
                            signalAvailableHandler();   //处理完了，就唤醒其他线程
                        } else {
                            logger.error("Can not connect to remote server, remote peer = " + remotePeer);
                        }
                    }
                });
            }
        });
    }

    /**
     * 唤醒其他线程
     */
    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     *  等待处理器
     * @return
     * @throws InterruptedException
     */
    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();  //加锁
        try {
            logger.warn("Waiting for available service");
            return connected.await(this.waitTimeout, TimeUnit.MILLISECONDS);   //调用await会释放锁
        } finally {
            lock.unlock();
        }
    }

    /**
     *   从包含serviceKey服务的服务器中，选择处理器
     * @param serviceKey
     * @return
     * @throws Exception
     */
    public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
        int size = connectedServerNodes.values().size();  //查看有没有处理器
        while (isRunning && size <= 0) {   //如果没有处理器，那么等一会
            try {
                waitingForHandler();  //等待处理器
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                logger.error("Waiting for available service is interrupted!", e);
            }
        }
        RpcProtocol rpcProtocol = loadBalance.route(serviceKey, connectedServerNodes);
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);  //获得处理器
        if (handler != null) {
            return handler;
        } else {
            throw new Exception("Can not get available connection");
        }
    }

    private void removeAndCloseHandler(RpcProtocol rpcProtocol) {
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler != null) {
            handler.close();
        }
        connectedServerNodes.remove(rpcProtocol);
        rpcProtocolSet.remove(rpcProtocol);
    }

    public void removeHandler(RpcProtocol rpcProtocol) {
        rpcProtocolSet.remove(rpcProtocol);
        connectedServerNodes.remove(rpcProtocol);
        logger.info("Remove one connection, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
    }

    public void stop() {
        isRunning = false;
        for (RpcProtocol rpcProtocol : rpcProtocolSet) {
            removeAndCloseHandler(rpcProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}
