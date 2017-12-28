package org.tron.consensus.client;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.client.ConnectionStrategies;
import io.atomix.copycat.client.CopycatClient;
import org.tron.consensus.common.GetQuery;
import org.tron.consensus.common.PutCommand;
import org.tron.overlay.message.Message;
import org.tron.overlay.message.Type;
import org.tron.peer.Peer;

import java.nio.channels.InterruptedByTimeoutException;
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
    public static void getMessage(String key) {
        Object result = client.submit(new GetQuery(key)).join();
        System.out.println("Consensus " + key + " is: " + result);
//        client.submit(new GetQuery(key)).thenAccept(result -> {
//            System.out.println("Consensus " + key + " is: " + result);
//        });

    }

    public static void putMessage1(Message message) {
        if (message.getType() == Type.TRANSACTION) {
            System.out.println("transaction:" + message.getType().toString()
                    + "; type: " + message.getMessage().getClass().getSimpleName
                    () + "; message: " + message.getMessage());
            client.submit(new PutCommand("transaction", message.getMessage()));
            System.out.println("transaction: Put message success");
        }

        if (message.getType() == Type.BLOCK) {
            System.out.println("block:" + message.getType().toString()
                    + "; type: " + message.getMessage().getClass().getSimpleName
                    () + "; message:" + message.getMessage());
            client.submit(new PutCommand("block", message.getMessage()));
            System.out.println("Block: Put Block success");
        }
    }

    public static void getMessage1(String key)  {

        Peer peerConsensus = Peer.getInstance("server");
        final String[] preMessage = {null};
        if (key.equals("transaction")) {
            client.submit(new GetQuery(key)).thenAccept(result -> {
                System.out.println("Consensus " + key + " is: " +
                        result);
                //System.out.println("type: " + result.getClass().getSimpleName());
                peerConsensus.addReceiveTransaction(String.valueOf(result));
            });
        }

        if (key.equals("block")) {
            Thread thread = new Thread(() -> {
                while(true){
                    client.submit(new GetQuery(key)).thenAccept(result -> {
                        System.out.println("Consensus " + key + " is: " +
                                result);
                        if (!String.valueOf(result).equals(preMessage[0])) {
                            peerConsensus.addReceiveBlock(String.valueOf(result));
                            preMessage[0] = String.valueOf(result);
                        }else {
                            preMessage[0] = preMessage[0];
                        }
                    });
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }
}
