package org.tron.consensus.server;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import org.tron.consensus.common.GetQuery;
import org.tron.consensus.common.MapstateMachine;
import org.tron.consensus.common.PutCommand;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;

public class Server {
    public static void serverRun() {

        InetAddress localhost = null;
        try {
            localhost = InetAddress.getLocalHost();
            System.out.println("Server localhost: " + localhost.getHostAddress
                    ());

            Address address = new Address(localhost.getHostAddress(), 5000);

            CopycatServer server = CopycatServer.builder(address)
                    .withStateMachine(MapstateMachine::new)
                    .withTransport(NettyTransport.builder()
                            .withThreads(4)
                            .build())
                    .withStorage(Storage.builder()
                            .withDirectory(new File("consensus-logs"))
                            .withStorageLevel(StorageLevel.DISK)
                            .build())
                    .build();

            server.serializer().register(PutCommand.class);
            server.serializer().register(GetQuery.class);

            CompletableFuture<CopycatServer> future = server.bootstrap();
            future.join();

            //Collection<Address> cluster = Collections.singleton(new Address
            //        ("192.16.50.129", 5000));
            //server.join(cluster).join();

            System.out.println("Server xxd: " + server.cluster().members());

            CopycatServer.State state = server.state();
            System.out.println("Server state: " + state);
            server.onStateChange(state1 -> {
                if (state == CopycatServer.State.LEADER) {
                    System.out.println("Server state: " + state);
                }
            });
            server.context();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
