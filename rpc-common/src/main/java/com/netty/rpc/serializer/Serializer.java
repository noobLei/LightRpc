package com.netty.rpc.serializer;

/**
 *  所有序列化方式，通过继承这个抽象类，重写serialize和deserialize方法，自行扩展
 */
public abstract class Serializer {
    public abstract <T> byte[] serialize(T obj);

    public abstract <T> Object deserialize(byte[] bytes, Class<T> clazz);
}
