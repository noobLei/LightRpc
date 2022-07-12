package com.netty.rpc.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC annotation for RPC service  自定义RPC服务的注解
 *         这个注解加在需要对外暴露的服务实现类上
 *
 */
@Target({ElementType.TYPE})  //ElementType.TYPE接口、类、枚举、注解
@Retention(RetentionPolicy.RUNTIME)  //定义该自定义注解的保存范围是RUNTIME
@Component   //// 表明可被 Spring 扫描
public @interface NettyRpcService {
    Class<?> value();

    String version() default "";
}
