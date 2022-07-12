package com.netty.rpc.server.core;

import com.netty.rpc.codec.Beat;
import com.netty.rpc.codec.RpcRequest;
import com.netty.rpc.codec.RpcResponse;
import com.netty.rpc.util.ServiceUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import net.sf.cglib.reflect.FastClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * RPC Handler（RPC request processor）
 *      使用 RpcServerHandler 来处理 RPC 请求，只需扩展 Netty 的 SimpleChannelInboundHandler 抽象类即可
 *  自定义处理类  实现其userEventTriggered()方法，在出现超时事件时会被触发
 *
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

//    LoggerFactory.getLogger可以在IDE控制台打印日志，便于开发，一般加在代码最上面
    private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);

    private final Map<String, Object> handlerMap;  //key：服务端为接口生成的，根据接口名和版本生成，具体调用ServiceUtil类的方法
    private final ThreadPoolExecutor serverHandlerPool;  //服务器的线程池

    public RpcServerHandler(Map<String, Object> handlerMap, final ThreadPoolExecutor threadPoolExecutor) {
        this.handlerMap = handlerMap;
        this.serverHandlerPool = threadPoolExecutor;
    }

    /**
     * netty的channel中两种方法
     * 可以很明显的看到，channelRead 是public 类型，可以被外部访问；而channelRead0是protected类型，只能被当前类及其子类访问。
     * 而且channelRead实际上也是调用channelRead0，只不过会先进行一个消息类型检查，判断当前message 是否需要传递到下一个handler。
     * @param ctx
     * @param request
     */
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final RpcRequest request) {
        // filter beat ping
        //说明是客户端发送的心跳
        if (Beat.BEAT_ID.equalsIgnoreCase(request.getRequestId())) {
            logger.info("Server read heartbeat ping");
            return;
        }

        //将请求任务提交给线程池进行处理
        serverHandlerPool.execute(new Runnable() {
            @Override
            public void run() {
                logger.info("Receive request " + request.getRequestId());
                RpcResponse response = new RpcResponse();
                response.setRequestId(request.getRequestId());  //客户端定义为响应id，可读性会强一点
                try {
                    Object result = handle(request);  //处理请求
                    response.setResult(result);
                } catch (Throwable t) {
                    response.setError(t.toString());
                    logger.error("RPC Server handle request error", t);
                }
                //ctx.writeAndFlush(response)返回是ChannelFuture对象，
//                通过ChannelFuture我们可以添加Listener，那么在消息发送完成后会进行回调，我们再去处理业务逻辑。
                ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        logger.info("Send response for request " + request.getRequestId());
//                        ctx.close();  要关闭通道嘛？
                    }
                });
            }
        });
    }

    /**
     * 对客户端的请求进行处理，根据map找到对应的服务接口的全限定类名，再利用反射，去调用服务的特定的方法
     * @param request
     * @return
     * @throws Throwable
     */
    private Object handle(RpcRequest request) throws Throwable {
        //获取客户端发送的请求消息：需要调用哪个类的，哪个方法，
        String className = request.getClassName();
        String version = request.getVersion();
        String serviceKey = ServiceUtil.makeServiceKey(className, version);
        Object serviceBean = handlerMap.get(serviceKey);  //获取到客户端真正想调用的服务端的哪个bean对象
        if (serviceBean == null) {
            logger.error("Can not find service implement with interface name: {} and version: {}", className, version);
            return null;
        }

        Class<?> serviceClass = serviceBean.getClass();  //获取class对象
        String methodName = request.getMethodName();  //获取方法的名称
        Class<?>[] parameterTypes = request.getParameterTypes();  //获取参数类型
        Object[] parameters = request.getParameters();  //获取参数

        logger.debug(serviceClass.getName());  //打印服务全限定类名
        logger.debug(methodName);
        for (int i = 0; i < parameterTypes.length; ++i) {
            logger.debug(parameterTypes[i].getName());
        }
        for (int i = 0; i < parameters.length; ++i) {
            logger.debug(parameters[i].toString());
        }
        /**
         * 两个方式jdk反射和cglib反射调用具体的方法  看谁的性能号，就用谁
         */
        // JDK reflect
//        Method method = serviceClass.getMethod(methodName, parameterTypes);
//        method.setAccessible(true);
//        return method.invoke(serviceBean, parameters);

        // Cglib reflect
        FastClass serviceFastClass = FastClass.create(serviceClass);
//        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
//        return serviceFastMethod.invoke(serviceBean, parameters);

        // for higher-performance
        int methodIndex = serviceFastClass.getIndex(methodName, parameterTypes);
        return serviceFastClass.invoke(methodIndex, serviceBean, parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Server caught exception: " + cause.getMessage());
        //答应错误信息之后再关闭通道
        ctx.close();
    }

    /**
     * netty心跳机制，通过userEventTriggered方法进行心跳检测，用户超时长时间未操作时则会触发，
     * 通过发送ping/pong的指令来保持客户端与服务端之间的连接不中断
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
            //通到在最后几秒将被关闭
            logger.warn("Channel idle in last {} seconds, close it", Beat.BEAT_TIMEOUT);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
