package com.app.test;

import com.app.test.service.Foo;
import com.app.test.service.HelloService;
import com.app.test.service.Person;
import com.app.test.service.PersonService;
import com.netty.rpc.client.RpcClient;
import com.netty.rpc.client.handler.RpcFuture;
import com.netty.rpc.client.proxy.RpcFunction;
import com.netty.rpc.client.proxy.RpcFunction2;
import com.netty.rpc.client.proxy.RpcService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:client-spring.xml")
public class ServiceTest2 {
    @Autowired
    private Foo foo;

    @Autowired
    private RpcClient rpcClient;

    @After
    public void stop() {
        rpcClient.stop();
    }

    @Test
    public void say() {
        String result = foo.say("Foo");
        Assert.assertEquals("Hello Foo", result);
    }

    @Test
    public void serviceTest1() throws Exception {
        RpcService<HelloService, String, RpcFunction<HelloService, String>> helloService =
                rpcClient.createAsyncService(HelloService.class, "1.0");
        RpcFuture result = helloService.call(HelloService::hello, "World");
        System.out.println(result.get());
        Assert.assertEquals("Hello World", result.get());
    }

    @Test
    public void serviceTest2() throws Exception {
        RpcService<HelloService, String, RpcFunction2<HelloService, String, Integer>> helloService2 =
                rpcClient.createAsyncService(HelloService.class, "1.0");
        RpcFuture result = helloService2.call(HelloService::hello, "Tom", 2);
        System.out.println(result.get());
        Assert.assertEquals("Tom is 2", result.get());
    }

    @Test
    public void serviceTest3() throws Exception {
        RpcService<PersonService, String, RpcFunction2<PersonService, String, Integer>> helloService2 =
                rpcClient.createAsyncService(PersonService.class, "");
        Integer num = 2;
        RpcFuture result = helloService2.call(PersonService::callPerson, "Tom", num);
        List<Person> persons = (List<Person>) result.get();
        List<Person> expectedPersons = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            expectedPersons.add(new Person(Integer.toString(i), "Tom"));
        }
        assertThat(persons, equalTo(expectedPersons));

        for (int i = 0; i < num; ++i) {
            System.out.println(persons.get(i));
        }
    }
}
