package com.netty.rpc.client.route.impl;

import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.client.route.RpcLoadBalance;
import com.netty.rpc.protocol.RpcProtocol;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round robin load balance
 *
 */
public class RpcLoadBalanceRoundRobin extends RpcLoadBalance {
    //原子类修改操作
    private AtomicInteger roundRobin = new AtomicInteger(0);

    /**
     * 对拥有某个服务的服务器集合做轮询策略
     * @param addressList
     * @return
     */
    public RpcProtocol doRoute(List<RpcProtocol> addressList) {
        int size = addressList.size();
        // Round robin
        int index = (roundRobin.getAndAdd(1) + size) % size;
        return addressList.get(index);
    }

    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
        //调用父类RpcLoadBalance的方法，得到服务和服务器的map集合
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcProtocol> addressList = serviceMap.get(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
