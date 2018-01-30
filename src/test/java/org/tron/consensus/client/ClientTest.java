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
import io.atomix.copycat.client.CopycatClient;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.tron.core.consensus.common.GetQuery;
import org.tron.core.consensus.common.PutCommand;

public class ClientTest {

  public static void main(String[] args) {
    CopycatClient.Builder builder = CopycatClient.builder();

    builder.withTransport(NettyTransport.builder()
        .withThreads(2)
        .build());

    CopycatClient client = builder.build();

    client.serializer().register(PutCommand.class);
    client.serializer().register(GetQuery.class);

    InetAddress localhost = null;
    try {
      localhost = InetAddress.getLocalHost();
      System.out.println(localhost);
      Collection<Address> cluster = Arrays.asList(
          new Address(localhost.getHostAddress(), 5000)
      );
      CompletableFuture<CopycatClient> future = client.connect(cluster);
      future.join();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    client.submit(new GetQuery("block")).thenAccept(result1 -> {
      System.out.println("foo is: " + result1.toString());
    });
    int i = 1;
    //客户端提交查询
    boolean f = true;
    while (f) {
      String key = "block" + i;
      Object result = client.submit(new GetQuery(key)).join();
      try {
        if (!(result == null)) {
          System.out.println("空指针异常没有发生，为null");
          System.out.println("Consensus " + key + " is: " + result);
          f = true;
          i = i + 1;
        } else {
          f = false;
        }

      } catch (NullPointerException e) {
        System.out.println("object == null不会导致空指针异常发生");
        f = false;
      }

    }
  }
}
