package com.netty.rpc.util;

/**
 *  生成服务端的serviceKey
 */
public class ServiceUtil {
    public static final String SERVICE_CONCAT_TOKEN = "#";

    public static String makeServiceKey(String interfaceName, String version) {
        String serviceKey = interfaceName;
        if (version != null && version.trim().length() > 0) {
            serviceKey += SERVICE_CONCAT_TOKEN.concat(version);
        }
        return serviceKey;
    }
}
