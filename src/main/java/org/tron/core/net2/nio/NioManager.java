package org.tron.core.net2.nio;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NioManager {

    private static final Logger logger = LoggerFactory.getLogger("NioManager");

    private static CopyOnWriteArrayList<InetSocketAddress> blacklist = new CopyOnWriteArrayList<>();

    private static CopyOnWriteArrayList<InetSocketAddress> seedlist = new CopyOnWriteArrayList<>();

    public static void connect(InetSocketAddress address){
        try{
            logger.info("connect to {}", address);
            new NioClient(address).connect();
        }catch (Exception e){
            logger.error("connect to {} failed" + address, e);
        }
    }

    public static void startServer(int port){
        try{
            logger.info("start server, port=" + port);
            new NioServer(port).start();
        }catch (Exception e){
            logger.error("start server failed, port=" + port, e);
        }
    }

    public static void addToBlacklist(InetSocketAddress address){
        blacklist.add(address);
        logger.info("add {} to blacklist, blacklist size is {}.", address.getAddress(), blacklist.size());
    }

    public static void setSeedlist(List<InetSocketAddress> seeds){
        if (!CollectionUtils.isEmpty(seeds)){
            seedlist.addAll(seeds);
        }
        logger.info("add seeds [{}]", seedlist);
    }

    public static List<InetSocketAddress> getSeedlist(){
        return seedlist;
    }

}
