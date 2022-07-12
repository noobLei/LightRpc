package com.netty.rpc.codec;

import com.netty.rpc.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC Encoder  自定义编码器（继承netty的MessageToByteEncoder）
 * 使用 RpcEncoder 提供 RPC 编码，只需扩展 Netty 的 MessageToByteEncoder 抽象类的 encode 方法即可，
 *
 *
 * @author luxiaoxun
 */
public class RpcEncoder extends MessageToByteEncoder {
    private static final Logger logger = LoggerFactory.getLogger(RpcEncoder.class);
    private Class<?> genericClass;    ////RpcRequest.class或者RpcResponse.class
    private Serializer serializer;   //序列化器

    public RpcEncoder(Class<?> genericClass, Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    /**
     *   功能：把对象in采用序列化生成字节数组，然后作为ByteBuf输出。
     * @param ctx
     * @param in
     * @param out
     * @throws Exception
     */
    @Override
    public void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)) {
            try {
                byte[] data = serializer.serialize(in);  //默认采用kryo序列化
                out.writeInt(data.length);  //先写消息的长度
                out.writeBytes(data);  //再写消息的数据
            } catch (Exception ex) {
                logger.error("Encode error: " + ex.toString());
            }
        }
    }
}
