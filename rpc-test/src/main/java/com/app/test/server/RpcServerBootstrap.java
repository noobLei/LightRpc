package com.app.test.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 有spring配置使用这个启动
 *   启动服务器并发布服务
 *   为了加载 Spring 配置文件（server-spring）来发布服务，只需编写一个引导程序即可：
 */
public class RpcServerBootstrap {
    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("server-spring.xml");
    }
}
