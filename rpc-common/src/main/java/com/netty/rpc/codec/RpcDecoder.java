package com.netty.rpc.codec;

import com.netty.rpc.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * RPC Decoder  自定义解码器（继承netty的ByteToMessageDecoder）
 *   使用 RpcDecoder 提供 RPC 解码，只需扩展 Netty 的 ByteToMessageDecoder 抽象类的 decode 方法即可
 * @author luxiaoxun
 */
public class RpcDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(RpcDecoder.class);
    private Class<?> genericClass;  //RpcRequest.class或者RpcResponse.class
    private Serializer serializer;  //序列化器，默认采用kryo Serializer，进行序列化和反序列

    public RpcDecoder(Class<?> genericClass, Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

//    本项目就是利用 “消息长度 + 消息内容” 方式解决TCP粘包、拆包问题的。
//    所以在解码时要判断数据是否够长度读取，没有不够说明数据没有准备好，继续读取数据并解码，这里这种方式可以获取一个个完整的数据包

    /**
     * ctx是当前解码的上下文对象，in为字节数据的来源，将从ByteBuf获取的字节数据转换为实际的数据类型后添加到out中。
     * 解码：先读长度，再读数据，因为编码的时候就是先发送长度再发送数据
     * @param ctx
     * @param in
     * @param out
     * @throws Exception
     */
    @Override
    public final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();  //标记ByteBuf读指针的位置
        int dataLength = in.readInt();   //读取数据内容的长度
        if (in.readableBytes() < dataLength) {  //消息内容
            in.resetReaderIndex();  //可读取的数据长度小于请求体的，直接丢弃，并重置读指针位置
            return;
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data);

        Object obj = null;
        try {
            //反序列化
            obj = serializer.deserialize(data, genericClass);  //序列化器，默认采用kryoSerializer，进行序列化和反序列

            out.add(obj);  //将反序列化生成的对象，放入集合中
        } catch (Exception ex) {
            logger.error("Decode error: " + ex.toString());
        }
    }

}
