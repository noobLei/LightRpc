package com.netty.rpc.server.core;

import com.netty.rpc.codec.*;
import com.netty.rpc.serializer.Serializer;
import com.netty.rpc.serializer.hessian.HessianSerializer;
import com.netty.rpc.serializer.kryo.KryoSerializer;
import com.netty.rpc.serializer.protostuff.ProtostuffSerializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *  NIO通道channel  初始化  用于在某个Channel注册到EventLoop后，对这个Channel执行一些初始化操作。
 */
public class RpcServerInitializer extends ChannelInitializer<SocketChannel> {
    private Map<String, Object> handlerMap;
    private ThreadPoolExecutor threadPoolExecutor;

    public RpcServerInitializer(Map<String, Object> handlerMap, ThreadPoolExecutor threadPoolExecutor) {
        this.handlerMap = handlerMap;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public void initChannel(SocketChannel channel) throws Exception {
//        Serializer serializer = ProtostuffSerializer.class.newInstance();  //Protostuff序列化
//        Serializer serializer = HessianSerializer.class.newInstance();  //Hessian序列化
        Serializer serializer = KryoSerializer.class.newInstance();  //默认采用kryoSerializer，进行序列化和反序列
        ChannelPipeline cp = channel.pipeline();
        //心跳机制主要是用来检测远端是否存活，如果不存活或活跃则对空闲Socket连接进行处理避免资源的浪费；
//        IdleStateHandler是Netty自带的心跳机制

        cp.addLast(new IdleStateHandler(0, 0, Beat.BEAT_TIMEOUT, TimeUnit.SECONDS));
//        LengthFieldBasedFrameDecoder类是Netty提供的用来解析带长度字段数据包的类，继承自ByteToMessageDecoder类。
//        LengthFieldBasedFrameDecoder作用是防止粘包。
        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
//        在netty中，编码器encoder就是出站处理器，解码decoder就是入站处理器。
//        因此，在使用编解码时，只需要将它们添加到ChannelPipeline中即可，但是要注意编解码添加的前后顺序。
        cp.addLast(new RpcDecoder(RpcRequest.class, serializer));  // 将 RPC 请求进行解码（为了处理请求）
        cp.addLast(new RpcEncoder(RpcResponse.class, serializer));  // 将 RPC 响应进行编码（为了返回响应）
        cp.addLast(new RpcServerHandler(handlerMap, threadPoolExecutor));  // 处理 RPC 请求
    }
}
