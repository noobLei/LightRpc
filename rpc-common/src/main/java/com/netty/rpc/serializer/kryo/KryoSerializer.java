package com.netty.rpc.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.netty.rpc.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class KryoSerializer extends Serializer {
    private KryoPool pool = KryoPoolFactory.getKryoPoolInstance();  //得到单例KryoPool

    /**
     * 序列化方法，把指定对象序列化成字节数组
     * @param obj
     * @param <T>
     * @return
     */
    @Override
    public <T> byte[] serialize(T obj) {
        Kryo kryo = pool.borrow();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output out = new Output(byteArrayOutputStream);
        try {
            kryo.writeObject(out, obj);
            out.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                byteArrayOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            pool.release(kryo);
        }
    }

    /**
     * 反序列化方法，将字节数组反序列化成指定Class类型
     * @param bytes
     * @param clazz
     * @param <T>
     * @return
     */
    @Override
    public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
        Kryo kryo = pool.borrow();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Input in = new Input(byteArrayInputStream);
        try {
            Object result = kryo.readObject(in, clazz);
            in.close();
            return result;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                byteArrayInputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            pool.release(kryo);
        }
    }
}
