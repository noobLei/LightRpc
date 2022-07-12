package com.app.test.client;

import com.netty.rpc.client.handler.AsyncRPCCallback;
import com.netty.rpc.client.handler.RpcFuture;
import com.netty.rpc.client.RpcClient;
import com.netty.rpc.client.proxy.RpcService;
import com.app.test.service.Person;
import com.app.test.service.PersonService;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 *
 */
public class RpcCallbackTest {
    public static void main(String[] args) {
        final RpcClient rpcClient = new RpcClient("10.217.59.164:2181");
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        try {
            RpcService client = rpcClient.createAsyncService(PersonService.class, "");
            int num = 5;
            RpcFuture helloPersonFuture = client.call("callPerson", "Jerry", num);
            helloPersonFuture.addCallback(new AsyncRPCCallback() {
                @Override
                public void success(Object result) {
                    List<Person> persons = (List<Person>) result;
                    for (int i = 0; i < persons.size(); ++i) {
                        System.out.println(persons.get(i));
                    }
                    countDownLatch.countDown();
                }

                @Override
                public void fail(Exception e) {
                    System.out.println(e);
                    countDownLatch.countDown();
                }
            });

        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        rpcClient.stop();

        System.out.println("End");
    }
}
