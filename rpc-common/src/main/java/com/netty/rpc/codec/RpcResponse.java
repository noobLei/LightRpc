package com.netty.rpc.codec;

import java.io.Serializable;

/**
 * RPC Response   RPC响应信息   服务端发送给客户端的响应请求
 *
 * @author luxiaoxun
 */
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 8215493329459772524L;

    private String requestId;    //客户端请求id
    private String error;   //错误信息
    private Object result;    //响应结果

    public boolean isError() {
        return error != null;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
