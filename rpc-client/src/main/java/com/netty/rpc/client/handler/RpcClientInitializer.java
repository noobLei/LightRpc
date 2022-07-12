package com.netty.rpc.client.handler;

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

import java.util.concurrent.TimeUnit;

/**
 *    通道初始化，添加一些系列的handler处理器
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
//        Serializer serializer = ProtostuffSerializer.class.newInstance();
//        Serializer serializer = HessianSerializer.class.newInstance();
        Serializer serializer = KryoSerializer.class.newInstance();
        //        addLast把一个业务处理类（handler） 添加到链中的最后一个位置
        ChannelPipeline cp = socketChannel.pipeline();
        cp.addLast(new IdleStateHandler(0, 0, Beat.BEAT_INTERVAL, TimeUnit.SECONDS));
        cp.addLast(new RpcEncoder(RpcRequest.class, serializer));  //对rpc请求进行编码
        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        cp.addLast(new RpcDecoder(RpcResponse.class, serializer)); //对rpc响应进行解码
        cp.addLast(new RpcClientHandler());
    }
}
