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

package org.tron.core.consensus.server;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import org.tron.common.application.Service;
import org.tron.core.consensus.common.GetQuery;
import org.tron.core.consensus.common.MapstateMachine;
import org.tron.core.consensus.common.PutCommand;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;

public class Server implements Service {

  private CopycatServer server;

  @Override
  public void init() {

  }

  @Override
  public void start() {
    InetAddress localhost = null;
    try {
      localhost = InetAddress.getLocalHost();
      System.out.println("Server localhost: " + localhost.getHostAddress());
      Address address = new Address(localhost.getHostAddress(), 5000);
      server = CopycatServer.builder(address)
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
      //        ("192.168.0.100", 5000));
      //server.join(cluster).join();

      System.out.println("Server xxd: " + server.cluster().members());
      CopycatServer.State state = server.state();
      System.out.println("Server state: " + state);
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void stop() {
    server.leave();
  }
}
