package com.app.test.service;

/**
 *  编写服务接口
 */
public interface HelloService {
    String hello(String name);

    String hello(Person person);

    String hello(String name, Integer age);
}
