package org.tron.overlay.kafka;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.overlay.Net;
import org.tron.overlay.listener.ReceiveSource;
import org.tron.overlay.message.Message;
import org.tron.overlay.message.Type;

import java.util.Arrays;

import static org.tron.core.Constant.TOPIC_BLOCK;
import static org.tron.core.Constant.TOPIC_TRANSACTION;

public class KafkaTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testKafka() {
        ReceiveSource source = new ReceiveSource();
        source.addReceiveListener((Message message) -> {
            if (message.getType() == Type.BLOCK) {
                System.out.println(message.getMessage());
            }
        });

        source.addReceiveListener((Message message) -> {
            if (message.getType() == Type.TRANSACTION) {
                System.out.println(message.getMessage());
            }
        });

        Net net = new Kafka(source, Arrays.asList(TOPIC_BLOCK, TOPIC_TRANSACTION));

        net.broadcast(new Message("hello block", Type.BLOCK));
        net.broadcast(new Message("hello transaction", Type.TRANSACTION));

        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis();

        while (endTime - startTime < 50000) {
            endTime = System.currentTimeMillis();
        }
    }
}
