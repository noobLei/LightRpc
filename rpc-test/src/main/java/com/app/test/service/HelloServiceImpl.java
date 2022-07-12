package com.app.test.service;

import com.netty.rpc.annotation.NettyRpcService;

/**
 * 服务接口实现类
 * 使用 NettyRpcService 注解定义在服务接口的实现类上，需要对该实现类指定远程接口，
 * 因为实现类可能会实现多个接口，一定要告诉框架哪个才是远程接口。
 * 因为一个接口可能有多个实现类，因此需要添加一个版本
 */
//意思是这个实现类是HelloService接口的，版本为1.0
@NettyRpcService(value = HelloService.class, version = "1.0")
public class HelloServiceImpl implements HelloService {

    public HelloServiceImpl() {

    }

    @Override
    public String hello(String name) {
        return "Hello " + name;
    }

    @Override
    public String hello(Person person) {
        return "Hello " + person.getFirstName() + " " + person.getLastName();
    }

    @Override
    public String hello(String name, Integer age) {
        return name + " is " + age;
    }
}
