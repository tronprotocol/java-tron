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

import java.net.InetAddress;
import java.net.UnknownHostException;
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
                new Address("192.168.0.100", 5000)
        );
        CompletableFuture<CopycatClient> future = client.connect(cluster);
        future.join();
        /*InetAddress localhost = null;
        try {
            localhost = InetAddress.getLocalHost();
            Collection<Address> cluster = Arrays.asList(
                    new Address(localhost.getHostAddress(), 5000)
            );

            CompletableFuture<CopycatClient> future = client.connect(cluster);
            future.join();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }*/
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
        Object result = client.submit(new GetQuery(key)).join();
        System.out.println("Consensus " + key + " is: " + result);
//        client.submit(new GetQuery(key)).thenAccept(result -> {
//            System.out.println("Consensus " + key + " is: " + result);
//        });

    }

    public static void putMessage1(Message message) {
        if (message.getType() == Type.TRANSACTION) {
            /*
            System.out.println("transaction:" + message.getType().toString()
                    + "; type: " + message.getMessage().getClass().getSimpleName
                    () + "; message: " + message.getMessage()); */
            client.submit(new PutCommand("transaction", message.getMessage()));
            client.submit(new PutCommand("time", System.currentTimeMillis()));
            System.out.println("transaction: consensus success");
        }

        if (message.getType() == Type.BLOCK) {
            /*
            System.out.println("block:" + message.getType().toString()
                    + "; type: " + message.getMessage().getClass().getSimpleName
                    () + "; message:" + message.getMessage());*/

            //client.submit(new PutCommand("block", message.getMessage()));
            //System.out.println("Block: consensus success");

            int i = 1;
            boolean f = true;
            while(f){
                String block_key = "block" + i;
                Object block = client.submit(new GetQuery(block_key)).join();
                try {
                    if (!(block == null)) {
                        f =true;
                        i = i+1;
                    }else {
                        client.submit(new PutCommand(block_key, message.getMessage()));
                        System.out.println("Block: consensus success");
                        f = false;
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    System.out.println("object == null");
                }
            }
        }
    }

    public static void getMessage(String key)  {

        Peer peerConsensus = Peer.getInstance("server");
        final String[] preMessage = {null};
        final String[] preTime = {null};
        if (key.equals("transaction")) {
            Thread thread = new Thread(() -> {
                while(true){
                    Object time = client.submit(new GetQuery("time")).join();
                    if(!time.toString().equals(preTime[0])) {
                        client.submit(new GetQuery(key)).thenAccept(transaction
                                -> {
                            //System.out.println("Consensus " + key + " is: " + result);
                            //System.out.println("type: " + result.getClass().getSimpleName());
                            peerConsensus.addReceiveTransaction(String
                                    .valueOf(transaction));
                        });
                        preTime[0] = time.toString();
                    }else {
                        preTime[0] = preTime[0];
                    }
                    try {
                        Thread.sleep(4000);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }

        if (key.equals("block")) {
            Thread thread = new Thread(() -> {
                while(true){
                    client.submit(new GetQuery(key)).thenAccept(block -> {
                        /*System.out.println("Consensus " + key + " is: " +
                                block);*/
                        if (!String.valueOf(block).equals(preMessage[0])) {
                            peerConsensus.addReceiveBlock(String.valueOf
                                    (block));
                            preMessage[0] = String.valueOf(block);
                        }else {
                            preMessage[0] = preMessage[0];
                        }
                    });
                    try {
                        Thread.sleep(4000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }
    public static void loadBlock(){
        Peer peerConsensus = Peer.getInstance("server");
        int i = 1;
        boolean f = true;
        while(f){
            String block_key = "block" + i;
            Object block = client.submit(new GetQuery(block_key)).join();
            try {
                if (!(block == null)) {
                    /*System.out.println("Consensus " + block_key + " is: " +
                                block);*/
                    peerConsensus.addReceiveBlock(String.valueOf
                            (block));
                    f =true;
                    i = i+1;
                }else {
                    f = false;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                System.out.println("object == null");
            }
        }
    }
}
