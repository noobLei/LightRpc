package com.netty.rpc.protocol;

import com.netty.rpc.util.JsonUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 *  RpcProtocol：可构建服务器集群，这是服务提供者的相关信息，ip地址，端口号，以及服务列表
 */
public class RpcProtocol implements Serializable {
    private static final long serialVersionUID = -1102180003395190700L;
    // 服务IP地址
    private String host;
    // 服务端口号
    private int port;
    // 所有服务信息列表
    private List<RpcServiceInfo> serviceInfoList;  //列表每个元素是RpcServiceInfo对象：每个服务的信息

    public String toJson() {
        String json = JsonUtil.objectToJson(this);
        return json;
    }

    public static RpcProtocol fromJson(String json) {
        return JsonUtil.jsonToObject(json, RpcProtocol.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcProtocol that = (RpcProtocol) o;
        return port == that.port &&
                Objects.equals(host, that.host) &&
                isListEquals(serviceInfoList, that.getServiceInfoList());
    }

    /**
     * 判断两个list是否相等
     * @param thisList
     * @param thatList
     * @return
     */
    private boolean isListEquals(List<RpcServiceInfo> thisList, List<RpcServiceInfo> thatList) {
        if (thisList == null && thatList == null) {
            return true;
        }
        if ((thisList == null && thatList != null)
                || (thisList != null && thatList == null)
                || (thisList.size() != thatList.size())) {
            return false;
        }
        return thisList.containsAll(thatList) && thatList.containsAll(thisList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, serviceInfoList.hashCode());
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<RpcServiceInfo> getServiceInfoList() {
        return serviceInfoList;
    }

    public void setServiceInfoList(List<RpcServiceInfo> serviceInfoList) {
        this.serviceInfoList = serviceInfoList;
    }
}
