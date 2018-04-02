package org.tron.core.net2.peer;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.net2.message.TMessage;
import org.tron.core.net2.message.TMessageType;

import java.util.Timer;
import java.util.TimerTask;

@Slf4j(topic = "core.net2")
public class PeerDiscover {

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
