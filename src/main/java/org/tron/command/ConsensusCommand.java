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
package org.tron.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.consensus.client.Client;
import org.tron.consensus.client.MessageType;
import org.tron.consensus.server.Server;
import org.tron.core.TransactionUtils;
import org.tron.example.Tron;
import org.tron.overlay.message.Message;
import org.tron.overlay.message.Type;
import org.tron.peer.Peer;
import org.tron.peer.PeerType;
import org.tron.protos.core.TronTransaction;
import org.tron.utils.ByteArray;

import static org.fusesource.jansi.Ansi.ansi;

public class ConsensusCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger("ConsensusCommand");

    public void server() {
        Server.serverRun();
    }

    public void putClient(String[] args) {
        Client.putMessage(args);
    }

    public void getClient(Peer peer) {

        if (Tron.getPeer().getType().equals(PeerType.PEER_SERVER)) {
            Client.getMessage(peer,MessageType.TRANSACTION);
            Client.getMessage(peer,MessageType.BLOCK);
        }else{
            Client.getMessage(peer,MessageType.BLOCK);
        }
    }

    public void listen(Peer peer,String type) {
        //Client.getMessage(args[0]);
        if (type.equals(PeerType.PEER_SERVER)) {
            Client.getMessage(peer,MessageType.TRANSACTION);
            Client.getMessage(peer,MessageType.BLOCK);
        }else{
            Client.getMessage(peer,MessageType.BLOCK);
        }
    }

    public void usage() {
        System.out.println("");

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold USAGE|@\n\t@|bold listen [key]|@"
        ) );

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold DESCRIPTION|@\n\t@|bold The command 'listen' " +
                        "listen consensus message.|@"
        ) );

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold USAGE|@\n\t@|bold send [key] [value]|@"
        ) );

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold DESCRIPTION|@\n\t@|bold The command 'send' " +
                        "send a transaction.|@"
        ) );

        System.out.println("");
    }

    @Override
    public void execute(Peer peer, String[] parameters) {
        if (check(parameters)) {
            String to = parameters[0];
            long amount = Long.valueOf(parameters[1]);
            TronTransaction.Transaction transaction = TransactionUtils
                    .newTransaction(peer.getWallet(), to, amount, peer.getUTXOSet());

            if (transaction != null) {
                Message message = new Message(ByteArray.toHexString
                        (transaction.toByteArray()), Type.TRANSACTION);
                Client.putMessage1(message);
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
