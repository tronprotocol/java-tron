package org.tron.core.net2.nio;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.nio.channels.SocketChannel;
import java.util.Objects;

public final class Address {

    private final String ip;
    private final int port;

    private Address(@CheckForNull String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public static Address create(String host, int port) {
        return new Address(host, port);
    }

    public static Address create(SocketChannel channel) throws  Exception {
        String str = channel.getRemoteAddress().toString().replace("/", "");
        String [] sz = str.split(":");
        return new Address(sz[0], Integer.valueOf(sz[1]));
    }

    @Nonnull
    public String ip() {
        return ip;
    }

    public int port() {
        return port;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Address that = (Address) other;
        return Objects.equals(ip, that.ip) && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    @Override
    public String toString() {
        return ip + ":" + port;
    }
}

