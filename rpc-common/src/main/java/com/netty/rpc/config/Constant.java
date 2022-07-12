package com.netty.rpc.config;

/**
 * ZooKeeper constant
 *     Constant 配置了所有的常量：
 *     首先需要使用 ZooKeeper 客户端命令行创建 /registry 永久节点，用于存放所有的服务临时节点。
 * @author luxiaoxun
 */
public interface Constant {
    int ZK_SESSION_TIMEOUT = 5000;  //zookeeper会话超时时间，就是zookeeper和客户端连接会存在一个会话
    int ZK_CONNECTION_TIMEOUT = 5000;

    String ZK_REGISTRY_PATH = "/registry";
    String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";

    String ZK_NAMESPACE = "netty-rpc";  //就是在zookeeper的netty-rpc节点下
}
