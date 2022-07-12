package com.netty.rpc.client.handler;

import com.netty.rpc.client.connect.ConnectionManager;
import com.netty.rpc.codec.Beat;
import com.netty.rpc.codec.RpcRequest;
import com.netty.rpc.codec.RpcResponse;
import com.netty.rpc.protocol.RpcProtocol;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 *      RpcClientHandler 客户端处理器
 *  *      使用 RpcClientHandler 来处理 RPC 响应，只需扩展 Netty 的 SimpleChannelInboundHandler 抽象类即可
 *  *  自定义处理类  实现其userEventTriggered()方法，在出现超时事件时会被触发
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);
    //key为请求的id，value为RpcFuture对象
    private ConcurrentHashMap<String, RpcFuture> pendingRPC = new ConcurrentHashMap<>();
    private volatile Channel channel;
    private SocketAddress remotePeer;  //服务提供者的套接字
    private RpcProtocol rpcProtocol;   //服务提供者

//    当客户端与服务端连接建立时调用
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remotePeer = this.channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }



    /**
     * //    从客户端接收到数据（响应）时调用，所以有响应数据了，就将数据调用 rpcFuture.done(response)进行处理;
     * @param ctx
     * @param response
     * @throws Exception
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        String requestId = response.getRequestId();
        logger.debug("Receive response: " + requestId);
        RpcFuture rpcFuture = pendingRPC.get(requestId);  //从队列中得到服务提供者返回的响应结果
        if (rpcFuture != null) {
            pendingRPC.remove(requestId);
            rpcFuture.done(response);     //获得数据之后
        } else {
            logger.warn("Can not get pending response for request id: " + requestId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Client caught exception: " + cause.getMessage());
        ctx.close();
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 向服务器（服务提供者）发送请求，并返回响应结果
     * 由代理类调用这个方法
     *   //为什么响应结果怎么封装进去RpcFuture（自定义的future）？
     *   因为上面的channelRead0方法，会在客户端接收到响应数据之后执行，方法体中把响应封装到RpcFuture中去了
     *  其实我觉得可以直接通过channelFuture返回结果的，但是不知道为啥还要自定义future
     * @param request
     * @return
     */
    public RpcFuture sendRequest(RpcRequest request) {
        RpcFuture rpcFuture = new RpcFuture(request);
        //把响应结果保存在pendingRPC中
        pendingRPC.put(request.getRequestId(), rpcFuture);
        try {
//            ChannelFuture的作用是用来保存Channel异步操作的结果。
//            调用sync()方法，等待异步操作执行完毕，就是让线程阻塞在这里，直到异步处理完成
            //发送请求（向服务器）
            ChannelFuture channelFuture = channel.writeAndFlush(request).sync();  //这个channelFuture不就可以拿到响应结果嘛，为啥还要RpcFuture
            if (!channelFuture.isSuccess()) {
                logger.error("Send request {} error", request.getRequestId());
            }
        } catch (InterruptedException e) {
            logger.error("Send request exception: " + e.getMessage());
        }

        return rpcFuture;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            //Send ping
            sendRequest(Beat.BEAT_PING);
            logger.debug("Client send beat-ping to " + remotePeer);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    public void setRpcProtocol(RpcProtocol rpcProtocol) {
        this.rpcProtocol = rpcProtocol;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ConnectionManager.getInstance().removeHandler(rpcProtocol);
    }
}
