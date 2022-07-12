package com.netty.rpc.codec;

/**
 * 心跳机制
 * 客户端发起心跳：客户端每隔一段时间发送策略消息给Socket服务器，Socket服务器原路返回策略消息，如果客户端在设定时间内没有收到返回的消息，经重试机制后，判定Socket服务器已经down，关闭连接。
 * 服务端发起的心跳：服务端实时记录每条Socket的IO操作时间，每隔一段时间获取所有Socket列表的快照，扫描每条Socket，如果该Socket的IO操作时间距当前时间已超出设定值，则判定客户端Down，关闭连接。
 */
public final class Beat {

    public static final int BEAT_INTERVAL = 30;  //INTERVAL：时间间隔
    public static final int BEAT_TIMEOUT = 3 * BEAT_INTERVAL;
    public static final String BEAT_ID = "BEAT_PING_PONG";

    public static RpcRequest BEAT_PING;

    static {
        BEAT_PING = new RpcRequest() {};
        BEAT_PING.setRequestId(BEAT_ID);
    }

}
