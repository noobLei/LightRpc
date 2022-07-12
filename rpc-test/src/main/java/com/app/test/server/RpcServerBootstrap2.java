package com.app.test.server;

import com.app.test.service.*;
import com.netty.rpc.server.RpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 没有spring配置用这个启动服务器
 */
public class RpcServerBootstrap2 {
    private static final Logger logger = LoggerFactory.getLogger(RpcServerBootstrap2.class);

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1:18866";  //zookeeper客户端（rpc 服务器）的地址
        String registryAddress = "192.168.245.159:2181"; //zookeeper服务器的地址
        RpcServer rpcServer = new RpcServer(serverAddress, registryAddress);
        HelloService helloService1 = new HelloServiceImpl();
        rpcServer.addService(HelloService.class.getName(), "1.0", helloService1);
        HelloService helloService2 = new HelloServiceImpl2();
        rpcServer.addService(HelloService.class.getName(), "2.0", helloService2);
        PersonService personService = new PersonServiceImpl();
        rpcServer.addService(PersonService.class.getName(), "", personService);
        try {
            rpcServer.start();
        } catch (Exception ex) {
            logger.error("Exception: {}", ex.toString());
        }
    }
}
