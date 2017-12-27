/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.consensus.client;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.client.ConnectionStrategies;
import io.atomix.copycat.client.CopycatClient;
import org.tron.consensus.common.GetQuery;
import org.tron.consensus.common.PutCommand;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class Client {

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
            System.out.println("Consensus " + key + " is: " +
                    result);
        });
    }
    public static void getMessage(String key) {
        Object result = client.submit(new GetQuery(key)).join();
        System.out.println("Consensus " + key + " is: " + result);

    }
}
