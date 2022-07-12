package com.netty.rpc.client.proxy;

/**
 * lambda method reference
 * @FunctionalInterface注解用来修饰interface(接口)
 * 被@FunctionalInterface修饰的接口，是一个新的函数接口，可以使用lambda表达式语法来使用
 * 该函数接口只能存在一个抽象方法
 */
//定义一个函数式接口，这样就可以使用lambda表达式
@FunctionalInterface
public interface RpcFunction2<T, P1, P2> extends SerializableFunction<T> {
    Object apply(T t, P1 p1, P2 p2);
}
