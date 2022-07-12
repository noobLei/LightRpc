package com.netty.rpc.server.core;

import com.netty.rpc.server.registry.ServiceRegistry;
import com.netty.rpc.util.ServiceUtil;
import com.netty.rpc.util.ThreadPoolUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *   使用 Netty 可实现一个支持 NIO 的 RPC 服务器，需要使用 ServiceRegistry 注册服务地址
 *
 */
public class NettyServer extends Server {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private Thread thread;
    private String serverAddress;  //zookeeper客户端（rpc 服务器）的地址（包括端口号）
    private ServiceRegistry serviceRegistry;
    private Map<String, Object> serviceMap = new HashMap<>();  // 存放接口名与服务对象之间的映射关系

    public NettyServer(String serverAddress, String registryAddress) {
        //zookeeper客户端（rpc 服务器）的地址  带端口号的
        this.serverAddress = serverAddress;
        //registryAddress ：zookeeper服务器的地址
        this.serviceRegistry = new ServiceRegistry(registryAddress);  //连接zookeeper服务器
    }

    /**
     * 添加服务
     * @param interfaceName
     * @param version
     * @param serviceBean   //注册哪个bean
     */
    public void addService(String interfaceName, String version, Object serviceBean) {
        logger.info("Adding service, interface: {}, version: {}, bean：{}", interfaceName, version, serviceBean);
        String serviceKey = ServiceUtil.makeServiceKey(interfaceName, version);  //为每个接口生成一个serviceKey
        serviceMap.put(serviceKey, serviceBean);  //服务和服务的bean对象存在map中，用于对客户端的请求进行处理
    }

    /**
     *  服务端启动的时候，创建线程池用于处理客户端请求，创建netty服务端，把服务注册到zookeeper上
     */
    public void start() {

        thread = new Thread(new Runnable() {
            //创建线程池
            ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtil.makeServerThreadPool(
                    NettyServer.class.getSimpleName(), 16, 32);

            @Override
            public void run() {
                //NIO
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    //Netty 中 ServerBootstrap 是服务端启动引导类 , Bootstrap 类是客户端程序的启动引导类，
                    ServerBootstrap bootstrap = new ServerBootstrap();
//                    new RpcServerInitializer(serviceMap, threadPoolExecutor)对通道进行一些初始化操作，
                    bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                            .childHandler(new RpcServerInitializer(serviceMap, threadPoolExecutor))
                            .option(ChannelOption.SO_BACKLOG, 128)
                            .childOption(ChannelOption.SO_KEEPALIVE, true);
//                    serverAddress 这是rpc服务器的IP地址和端口号
                    String[] array = serverAddress.split(":");   //127.0.0.1:18866
                    String host = array[0];
                    int port = Integer.parseInt(array[1]);
//                    bind该方法用于服务器端，用来设置占用的端口号
//                    因为这个代码是封装的，不仅仅是绑定端口号，更是建立连接，server.accpet()；这个是阻塞的，所以是异步；
                    ChannelFuture future = bootstrap.bind(host, port).sync();  //sync()等待异步操作执行完毕

                    if (serviceRegistry != null) {
                        //将服务集合注册到zookeeper服务器上  serviceMap：接口名和bean对象
                        serviceRegistry.registerService(host, port, serviceMap);   //向zookeeper注册服务地址
                    }
                    logger.info("Server started on port {}", port);
                    future.channel().closeFuture().sync();  //sync()等待异步操作执行完毕
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        logger.info("Rpc server remoting server stop");
                    } else {
                        logger.error("Rpc server remoting server error", e);
                    }
                } finally {
                    try {
                        serviceRegistry.unregisterService();
                        workerGroup.shutdownGracefully();
                        bossGroup.shutdownGracefully();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            }
        });
        thread.start();
    }

    public void stop() {
        // destroy server thread
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

}
