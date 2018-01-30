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

package org.tron.core.consensus.client;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.client.ConnectionStrategies;
import io.atomix.copycat.client.CopycatClient;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.Type;
import org.tron.core.consensus.common.GetQuery;
import org.tron.core.consensus.common.PutCommand;
import org.tron.core.peer.Peer;

public class Client {

  private CopycatClient client = null;

  public Client() {
    this.buildClient();
  }

  private void buildClient() {
    client = CopycatClient.builder()
        .withTransport(NettyTransport.builder()
            .withThreads(2)
            .build())
        .withConnectionStrategy(ConnectionStrategies.FIBONACCI_BACKOFF)
        .build();

    client.serializer().register(PutCommand.class);
    client.serializer().register(GetQuery.class);

    /*Collection<Address> cluster = Arrays.asList(
            new Address("192.168.0.102", 5000)
    );
    CompletableFuture<CopycatClient> future = client.connect(cluster);
    future.join();*/

    InetAddress localhost = null;
    try {
      localhost = InetAddress.getLocalHost();
      Collection<Address> cluster = Arrays.asList(
          new Address(localhost.getHostAddress(), 5000)
      );

      CompletableFuture<CopycatClient> future = client.connect(cluster);
      future.join();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }

  public void putMessage(String[] args) {
    String key = args[0];
    String value = args[1];
    client.submit(new PutCommand(key, value));
    System.out.println("Send message success");
  }

  public void getMessage1(String key) {
    Object result = client.submit(new GetQuery(key)).join();
    System.out.println("Consensus " + key + " is: " + result);
  }

  public void putMessage1(Message message) {
    if (message.getType() == Type.TRANSACTION) {
            /*System.out.println("transaction:" + message.getType().toString()
                    + "; type: " + message.getMessage().getClass().getSimpleName
                    () + "; message: " + message.getMessage()); */
      client.submit(new PutCommand("transaction", message.getMessage()));
      client.submit(new PutCommand("time", System.currentTimeMillis()));
      System.out.println("transaction: consensus success");
    }

    if (message.getType() == Type.BLOCK) {
            /*System.out.println("block:" + message.getType().toString()
                    + "; type: " + message.getMessage().getClass().getSimpleName
                    () + "; message:" + message.getMessage());*/
      int i = 1;
      final boolean[] f = {true};
      while (f[0]) {
        String block_key = "block" + i;
        Object block = client.submit(new GetQuery(block_key)).join();
        try {
          if (!(block == null)) {
            f[0] = true;
            i = i + 1;
          } else {
            client.submit(new PutCommand(block_key, message
                .getMessage()));
            System.out.println("Block: consensus success");
            f[0] = false;
          }
        } catch (NullPointerException e) {
          e.printStackTrace();
          System.out.println("object == null");
        }
      }
    }
  }

  public void getMessage(Peer peer, String key) {
    final String[] preMessage = {null};
    final String[] preTime = {null};
    //preTime[0] = client.submit(new GetQuery("time")).join().toString();
    Object pretime = client.submit(new GetQuery("time")).join();
    try {
      if (!(pretime == null)) {
        preTime[0] = pretime.toString();
      }
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
    if (key.equals("transaction")) {
      Thread thread = new Thread(() -> {
        while (true) {
          Object time = client.submit(new GetQuery("time")).join();
          if (time == null) {
            preTime[0] = null;
          } else {
            if (!time.toString().equals(preTime[0])) {
              client.submit(new GetQuery(key)).thenAccept(transaction
                  -> {
                //System.out.println("type: " + result.getClass()
                // .getSimpleName());
                peer.addReceiveTransaction(String.valueOf
                    (transaction));
              });
              preTime[0] = time.toString();
            } else {
              preTime[0] = preTime[0];
            }
            try {
              Thread.sleep(3000);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      });
      thread.start();
    }

    if (key.equals("block")) {
      Thread thread = new Thread(() -> {
        while (true) {
          int i = 1;
          final boolean[] f = {true};
          String block_key;
          while (f[0]) {
            block_key = "block" + i;
            Object block = client.submit(new GetQuery(block_key))
                .join();
            try {
              if (!(block == null)) {
                f[0] = true;
                i = i + 1;
              } else {
                f[0] = false;
              }
            } catch (NullPointerException e) {
              e.printStackTrace();
            }
          }

          i = i - 1;
          String finalBlock_key = "block" + i;
          client.submit(new GetQuery(finalBlock_key)).thenAccept
              ((Object block) -> {
                        /*System.out.println("Consensus " + key + " is: " +
                        block);*/
                if (!String.valueOf(block).equals(preMessage[0])) {
                  peer.addReceiveBlock(String.valueOf(block));
                  preMessage[0] = String.valueOf(block);
                } else {
                  preMessage[0] = preMessage[0];
                }
              });
          try {
            Thread.sleep(3000);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
      thread.start();
    }
  }

  public void loadBlock(Peer peer) {
    int i = 1;
    final boolean[] f = {true};

    while (f[0]) {
      String block_key = "block" + i;
      client.submit(new GetQuery(block_key)).thenAccept((Object block) -> {
        if (!(block == null)) {
          peer.addReceiveBlock(String.valueOf(block));
          f[0] = true;
        } else {
          f[0] = false;
        }
      });
      i++;
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
