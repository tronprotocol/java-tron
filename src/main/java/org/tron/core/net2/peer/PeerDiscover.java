package org.tron.core.net2.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.net2.message.TMessage;
import org.tron.core.net2.message.TMessageType;

import java.util.Timer;
import java.util.TimerTask;

public class PeerDiscover {

    private static final Logger logger = LoggerFactory.getLogger("PeerDiscover");

    public void start(){
        new Timer().schedule(
            new TimerTask() {
                @Override
                public void run() {
                    logger.info("start discover");
                    TMessage msg = new TMessage((byte) 1, TMessageType.GET_PEERS, null, null);
                    PeerClient.getInstance().sendMsg(msg);
                }
            }, 15 * 1000, 60 * 1000
        );
    }
}
