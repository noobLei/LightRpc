package com.netty.rpc.server.registry;

import com.netty.rpc.config.Constant;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;
import com.netty.rpc.util.ServiceUtil;
import com.netty.rpc.zookeeper.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *   实现服务注册，注册到注册中心zookeeper
 *   使用 ZooKeeper 的java客户端（curator）可轻松实现服务注册功能
 *
 * @author luxiaoxun
 */
public class ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    //创建curatorClient对象（就是zookeeper为注册中心服务器，rpc服务器作为客户端向zookeeper注册服务）
    private CuratorClient curatorClient;  //自定义的zookeeper客户端
    private List<String> pathList = new ArrayList<>();   //路径集合

//    registryAddress ： zookeeper服务器的地址
    public ServiceRegistry(String registryAddress) {
        //根据zookeeper服务器的地址相当于连接到zookeeper服务器
        this.curatorClient = new CuratorClient(registryAddress, 5000);
    }

    /**
     *  将serviceMap的服务，全部注册到zookeeper服务器上
     *  这个方法在服务端启动的时候会被调用
     * @param host  zookeeper服务器的ip地址
     * @param port  端口
     * @param serviceMap  要注册的服务集合
     */
    public void registerService(String host, int port, Map<String, Object> serviceMap) {
        // 要注册到服务中心的服务对象
        List<RpcServiceInfo> serviceInfoList = new ArrayList<>();
        for (String key : serviceMap.keySet()) {  //遍历所有要注册的服务信息
            //key = #接口名或者#接口名+版本号
            String[] serviceInfo = key.split(ServiceUtil.SERVICE_CONCAT_TOKEN);  //SERVICE_CONCAT_TOKEN=#
            if (serviceInfo.length > 0) {
                RpcServiceInfo rpcServiceInfo = new RpcServiceInfo();  //创建一个对象封装信息
                rpcServiceInfo.setServiceName(serviceInfo[0]);
                if (serviceInfo.length == 2) {
                    rpcServiceInfo.setVersion(serviceInfo[1]);
                } else {
                    rpcServiceInfo.setVersion("");  //没有版本号
                }
                logger.info("Register new service: {} ", key);
                serviceInfoList.add(rpcServiceInfo);  //添加到集合中
            } else {
                logger.warn("Can not get service name and version: {} ", key);
            }
        }
        try {
            //把服务器的IP地址，端口，要暴露的服务接口等相关信息封装到rpcProtocol对象，然后把这个对象注册到zookeeper上
            RpcProtocol rpcProtocol = new RpcProtocol();
            rpcProtocol.setHost(host);
            rpcProtocol.setPort(port);
            rpcProtocol.setServiceInfoList(serviceInfoList);
            //服务接口的IP地址，端口，服务列表
            String serviceData = rpcProtocol.toJson();  //转成json格式的字符串
            byte[] bytes = serviceData.getBytes();
            String path = Constant.ZK_DATA_PATH + "-" + rpcProtocol.hashCode();
            //path=/registry/data-哈希码
            path = this.curatorClient.createPathData(path, bytes);   //注册到zookeeper服务器上去
            pathList.add(path);
            logger.info("Register {} new service, host: {}, port: {}", serviceInfoList.size(), host, port);
        } catch (Exception e) {
            logger.error("Register service fail, exception: {}", e.getMessage());
        }

        curatorClient.addConnectionStateListener(new ConnectionStateListener() {
            @Override   //监听连接状态的变化，
            public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                if (connectionState == ConnectionState.RECONNECTED) {
                    logger.info("Connection state: {}, register service after reconnected", connectionState);
                    registerService(host, port, serviceMap);  //如果重连zookeeper，那么需要重写注册服务到zookeeper服务器上
                }
            }
        });
    }

    /**
     * 删除服务
     */
    public void unregisterService() {
        logger.info("Unregister all service");  //删除所有服务，即把之前注册在zookeeper服务中心的节点进行删除
        for (String path : pathList) {
            try {
                this.curatorClient.deletePath(path);
            } catch (Exception ex) {
                logger.error("Delete service path error: " + ex.getMessage());
            }
        }
        this.curatorClient.close();
    }
}
