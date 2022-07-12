package com.netty.rpc.serializer.protostuff;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.netty.rpc.serializer.Serializer;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/***
 *   ProtostuffSerializer是Protostuff序列化器
 *  Objenesis来实例化对象，它是比 Java 反射更加强大。
 *
 */
public class ProtostuffSerializer extends Serializer {
    private Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();

    private Objenesis objenesis = new ObjenesisStd(true);

    @SuppressWarnings("unchecked")
    private <T> Schema<T> getSchema(Class<T> cls) {
        // for thread-safe
        return (Schema<T>) cachedSchema.computeIfAbsent(cls, RuntimeSchema::createFrom);
    }

    /**
     * 序列化方法，把指定对象序列化成字节数组
     * @param obj
     * @param <T>
     * @return
     */
    @Override
    public <T> byte[] serialize(T obj) {
        Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(cls);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    /**
     *  反序列化方法，将字节数组反序列化成指定Class类型
     *  Objenesis来实例化对象，它是比 Java 反射更加强大。
     * @param bytes
     * @param clazz
     * @param <T>
     * @return
     */
    @Override
    public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
        try {
            T message = (T) objenesis.newInstance(clazz);  //Objenesis来实例化对象，它是比 Java 反射更加强大。
            Schema<T> schema = getSchema(clazz);
            ProtostuffIOUtil.mergeFrom(bytes, message, schema);
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
