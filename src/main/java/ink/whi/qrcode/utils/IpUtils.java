package ink.whi.qrcode.utils;

import java.net.*;
import java.util.Enumeration;

/**
 * IpUtils: 用于获取本机的ip，防止硬编码
 */
public class IpUtils {
    public final static String DEFAULT_IP = "127.0.0.1";

    public static String getLocalIpByNetCard() {
        try {
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements(); ) {
                NetworkInterface item = e.nextElement();
                for (InterfaceAddress address : item.getInterfaceAddresses()) {
                    if (item.isLoopback() || !item.isUp()) {
                        continue;
                    }
                    if (address.getAddress() instanceof Inet4Address) {
                        Inet4Address inet4Address = (Inet4Address) address.getAddress();
                        return inet4Address.getHostAddress();
                    }
                }
            }
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            return DEFAULT_IP;
        }
    }

    private static volatile String ip;

    // fixme: 这里为什么要加锁
    public static String getLocalIP() {
        if (ip == null) {
            synchronized (IpUtils.class) {
                if (ip == null) {
                    ip = getLocalIpByNetCard();
                }
            }
        }
        return ip;
    }
}
