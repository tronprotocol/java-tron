package org.tron.consensus.client;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.client.ConnectionStrategies;
import io.atomix.copycat.client.CopycatClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.tron.consensus.common.GetQuery;
import org.tron.consensus.common.PutCommand;
import org.tron.overlay.kafka.ConsumerWorker;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class Client{

    private static CopycatClient client = null;

    static {
        client = CopycatClient.builder()
                .withTransport(NettyTransport.builder()
                        .withThreads(2)
                        .build())
                .withConnectionStrategy(ConnectionStrategies.FIBONACCI_BACKOFF)
                .build();

        client.serializer().register(PutCommand.class);
        client.serializer().register(GetQuery.class);

        Collection<Address> cluster = Arrays.asList(
                new Address("192.168.0.108", 5000)

        );

        CompletableFuture<CopycatClient> future = client.connect(cluster);
        future.join();
    }

    public static CopycatClient getClient() {
        return client;
    }

    public static void putMessage(String[] args) {
        String key = args[0];
        String value = args[1];
        client.submit(new PutCommand(key, value));
        System.out.println("Put message success");
    }

    public static void getMessage1(String key) {

        client.submit(new GetQuery(key)).thenAccept(result -> {
            System.out.println("Consensus " + key + " is: " + result);
        });
//        Thread thread = new Thread(() -> {
//            while (true) {
//                try {
//                    client.submit(new GetQuery(key)).thenAccept(result -> {
//                        System.out.println("Consensus " + key + " is: " +
//                                result);
//                    });
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
        //thread.start();
    }
    public static void getMessage(String key) {
        Object result = client.submit(new GetQuery(key)).join();
        System.out.println("Consensus " + key + " is: " + result);

    }
}
