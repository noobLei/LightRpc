package com.netty.rpc.client;

import com.netty.rpc.annotation.RpcAutowired;
import com.netty.rpc.client.proxy.RpcService;
import com.netty.rpc.client.proxy.ObjectProxy;
import com.netty.rpc.client.connect.ConnectionManager;
import com.netty.rpc.client.discovery.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *  RPC Client（Create RPC proxy）
 *  Java 提供的动态代理技术（jdk）实现 RPC客户端的代理
 */
public class RpcClient implements ApplicationContextAware, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private ServiceDiscovery serviceDiscovery;
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));

    /**
     *  调用服务发现
     * @param address
     */
    public RpcClient(String address) {
        this.serviceDiscovery = new ServiceDiscovery(address);
    }

    /**
     * 客户端想要调用的服务：interfaceClass， 版本号version
     * @param interfaceClass
     * @param version
     * @param <T>
     * @param <P>
     * @return  代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T, P> T createService(Class<T> interfaceClass, String version) {
//        Proxy.newProxyInstance三个参数，被代理类的类加载器，类接口数组，处理器handler
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T, P>(interfaceClass, version)   //new ObjectProxy 处理器
        );
    }

    /**
     * 创建异步服务
     * @param interfaceClass
     * @param version
     * @param <T>
     * @param <P>
     * @return
     */
    public static <T, P> RpcService createAsyncService(Class<T> interfaceClass, String version) {
        return new ObjectProxy<T, P>(interfaceClass, version);  //返回的是一个处理器
    }

    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }

    public void stop() {
        threadPoolExecutor.shutdown();
        serviceDiscovery.stop();
        ConnectionManager.getInstance().stop();
    }

    @Override
    public void destroy() throws Exception {
        this.stop();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Field[] fields = bean.getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    RpcAutowired rpcAutowired = field.getAnnotation(RpcAutowired.class);
                    if (rpcAutowired != null) {
                        String version = rpcAutowired.version();
                        field.setAccessible(true);
                        field.set(bean, createService(field.getType(), version));
                    }
                }
            } catch (IllegalAccessException e) {
                logger.error(e.toString());
            }
        }
    }
}

