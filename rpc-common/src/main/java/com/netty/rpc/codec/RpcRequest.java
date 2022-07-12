package com.netty.rpc.codec;

import java.io.Serializable;

/**
 * RPC Request   封装RPC请求，客户端向服务端发送的请求
 *
 * @author luxiaoxun
 */
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = -2524587347775862771L;

    private String requestId;  //客户端请求id
    private String className;  //类名
    private String methodName;  //方法名
    private Class<?>[] parameterTypes;  //参数类型
    private Object[] parameters;  //参数值
    private String version;   //版本

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}