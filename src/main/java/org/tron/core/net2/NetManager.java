package org.tron.core.net2;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.net2.message.TMessage;
import org.tron.core.net2.message.TMessageHandle;
import org.tron.core.net2.nio.EventHandle;
import org.tron.core.net2.nio.NioManager;
import org.tron.core.net2.peer.PeerClient;
import org.tron.core.net2.peer.PeerDiscover;
import org.tron.core.net2.peer.PeerMsgProccess;
import org.tron.core.net2.util.NetUtil;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class NetManager {

    private static final Logger logger = LoggerFactory.getLogger("NetManager");

    private static TMessageHandle msgHandle = TMessageHandle.getInstance();

    private static EventHandle eventHandle = EventHandle.getInstance();

    private static PeerClient peerClient = PeerClient.getInstance();

    public  static  void broadcastMsg(TMessage msg){
        if (msg != null){
            msg.setChannel(null);
            peerClient.sendMsg(msg);
        }
    }

    public static void sendMsg(TMessage msg){
        if (msg != null && msg.getChannel() != null){
            peerClient.sendMsg(msg);
        }
    }

    public static void startService(Integer serverPort, List<InetSocketAddress> seeds){
        try{
            msgHandle.init();
            eventHandle.init();
            peerClient.init();
            regMsgHandle(PeerMsgProccess.class.getMethod("processMsg", TMessage.class));
            if(serverPort != null){
                NioManager.startServer(serverPort);
            }
            if (!CollectionUtils.isEmpty(seeds)){
                seeds = NetUtil.deleteRepeatedAddress(seeds);
                NioManager.setSeedlist(seeds);
                seeds.forEach(seed -> NioManager.connect(seed));
            }
            new PeerDiscover().start();
        }catch (Exception e){
            logger.error("start service failed.", e);
        }
    }

    public static void regMsgHandle(Method method){
        try{
            msgHandle.regMsgHandle(method);
        }catch (Exception e){
            logger.info("reg msg handle failed {}.", method.getDeclaringClass());
        }
    }

    public  static  void  main(String[] args){

        List<InetSocketAddress> list = new ArrayList<>();
        list.add(new InetSocketAddress("127.0.0.1", 10001));
        list.add(new InetSocketAddress("127.0.0.1", 10001));
        //startService(10001,null);
        startService(null,list);
    }

}
