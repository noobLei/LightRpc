package com.netty.rpc.client.route;

import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;
import com.netty.rpc.util.ServiceUtil;
import org.apache.commons.collections4.map.HashedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  RPC负载均衡
 */
public abstract class RpcLoadBalance {
    // Service map: key为服务，value为拥有这个服务的服务器的集合
    protected Map<String, List<RpcProtocol>> getServiceMap(Map<RpcProtocol, RpcClientHandler> connectedServerNodes) {
//        HashedMap是apache的commons项目出的工具类，可以用于替代HashMap，增加了一些新的功能。
        Map<String, List<RpcProtocol>> serviceMap = new HashedMap<>();  //这里是同一个服务，找出所有的服务器
        if (connectedServerNodes != null && connectedServerNodes.size() > 0) {
            //获取每个服务器rpcProtocol下的服务接口列表，因为rpcProtocol可能是集群，所有多个服务器存在相同的服务
            for (RpcProtocol rpcProtocol : connectedServerNodes.keySet()) {
                //遍历每个服务接口列表
                for (RpcServiceInfo serviceInfo : rpcProtocol.getServiceInfoList()) {
                    String serviceKey = ServiceUtil.makeServiceKey(serviceInfo.getServiceName(), serviceInfo.getVersion());
                    List<RpcProtocol> rpcProtocolList = serviceMap.get(serviceKey);
                    if (rpcProtocolList == null) {  //不存在这个
                        rpcProtocolList = new ArrayList<>();
                    }
                    rpcProtocolList.add(rpcProtocol);
                    //putIfAbsent() 方法会先判断指定的键（key）是否存在，不存在则将键/值对插入到 HashMap 中。存在的化就不会插入
                    serviceMap.putIfAbsent(serviceKey, rpcProtocolList);
                }
            }
        }
        return serviceMap;
    }

    // Route the connection for service key
    //   连接哪个服务器，使用什么策略，重写这个方法就好了
    public abstract RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception;
}
