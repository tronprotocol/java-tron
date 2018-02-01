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

package org.tron.common.command;

import static org.fusesource.jansi.Ansi.ansi;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.CliApplication;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.Type;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.consensus.client.Client;
import org.tron.core.consensus.client.MessageType;
import org.tron.core.consensus.server.Server;
import org.tron.core.peer.Peer;
import org.tron.core.peer.PeerType;
import org.tron.protos.Protocal.Transaction;

public class ConsensusCommand extends Command {

  private static final Logger logger = LoggerFactory.getLogger("ConsensusCommand");

  private Client client;
  private Server server;

  @Inject
  public ConsensusCommand(Client client) {
    this.client = client;
  }

  public void putClient(String[] args) {
    client.putMessage(args);
  }

  public void getClient(Peer peer) {
    if (peer.getType().equals(PeerType.PEER_SERVER)) {
      client.getMessage(peer, MessageType.TRANSACTION);
      client.getMessage(peer, MessageType.BLOCK);
    } else {
      client.getMessage(peer, MessageType.BLOCK);
    }
  }

  public void listen(Peer peer, String type) {
    if (type.equals(PeerType.PEER_SERVER)) {
      client.getMessage(peer, MessageType.TRANSACTION);
      client.getMessage(peer, MessageType.BLOCK);
    } else {
      client.getMessage(peer, MessageType.BLOCK);
    }
  }

  public void usage() {
    System.out.println("");

    System.out.println("");

    System.out.println(ansi().eraseScreen().render(
        "@|magenta,bold USAGE|@\n\t@|bold listen [key]|@"
    ));

    System.out.println("");

    System.out.println(ansi().eraseScreen().render(
        "@|magenta,bold DESCRIPTION|@\n\t@|bold The command 'listen' " +
            "listen consensus message.|@"
    ));

    System.out.println("");

    System.out.println(ansi().eraseScreen().render(
        "@|magenta,bold USAGE|@\n\t@|bold send [key] [value]|@"
    ));

    System.out.println("");

    System.out.println(ansi().eraseScreen().render(
        "@|magenta,bold DESCRIPTION|@\n\t@|bold The command 'send' " +
            "send a transaction.|@"
    ));

    System.out.println("");
  }

  @Override
  public void execute(CliApplication app, String[] parameters) {
    Peer peer = app.getPeer();

    if (check(parameters)) {
      String to = parameters[0];
      long amount = Long.valueOf(parameters[1]);
      Transaction transaction = TransactionCapsule
          .newTransaction(peer.getWallet(), to, amount, peer.getUTXOSet());

      if (transaction != null) {
        Message message = new Message(ByteArray.toHexString
            (transaction.toByteArray()), Type.TRANSACTION);
        client.putMessage1(message);
      }
    }
  }

  @Override
  public boolean check(String[] parameters) {
    if (parameters.length < 2) {
      logger.error("missing parameter");
      return false;
    }

    if (parameters[0].length() != 40) {
      logger.error("address invalid");
      return false;
    }

    long amount = 0;
    try {
      amount = Long.valueOf(parameters[1]);
    } catch (NumberFormatException e) {
      logger.error("amount invalid");
      return false;
    }

    if (amount < 0) {
      logger.error("amount required a positive number");
      return false;
    }

    return true;
  }
}
