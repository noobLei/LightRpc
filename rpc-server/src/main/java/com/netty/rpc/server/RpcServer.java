package com.netty.rpc.server;

import com.netty.rpc.annotation.NettyRpcService;
import com.netty.rpc.server.core.NettyServer;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * RPC Server  实现 RPC 服务器
 *      RpcServer可以调用父类NettyServer的start方法和stop方法
 *      这个类中很多东西由父类NettyServer实现了，直接调用就好了
 * @author luxiaoxun
 */
public class RpcServer extends NettyServer implements ApplicationContextAware, InitializingBean, DisposableBean {

    public RpcServer(String serverAddress, String registryAddress) {
        super(serverAddress, registryAddress);
    }

    /**
     *  服务在启动的时候扫描得到所有的服务接口及其实现：
     * @param ctx
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        // 获取所有带有 NettyRpcService 注解的 Spring Bean
        // 目的是将这些服务注册到zookeeper上
//        getBeansWithAnnotation 获取IOC容器中使用了@NettyRpcService注解的spring bean
        //key是类名，value是这个类的bean对象
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(NettyRpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                //得到对应的注解上参数的值
                NettyRpcService nettyRpcService = serviceBean.getClass().getAnnotation(NettyRpcService.class);
//                nettyRpcService.value()得到注解的值（类的class对象），getName()是获取全限定类型（也就是服务接口的全限定类名）
                String interfaceName = nettyRpcService.value().getName();  //接口名
                String version = nettyRpcService.version();  //版本号
                //调用父类NettyServer的addServer方法注册到zookeeper
                super.addService(interfaceName, version, serviceBean);
            }
        }
    }

    /**
     * springbean的生命周期中，属性填充之后会调用afterPropertiesSet方法
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        super.start();
    }

    @Override
    public void destroy() {
        super.stop();
    }
}
