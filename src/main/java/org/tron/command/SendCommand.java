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
import org.tron.core.TransactionUtils;
import org.tron.overlay.message.Message;
import org.tron.overlay.message.Type;
import org.tron.peer.Peer;
import org.tron.protos.core.TronTransaction;
import org.tron.utils.ByteArray;

public class SendCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger("Command");

    public SendCommand() {
    }

    @Override
    public void execute(Peer peer, String[] parameters) {
        if (check(parameters)) {
            String to = parameters[0];
            long amount = Long.parseLong(parameters[1]);
            TronTransaction.Transaction transaction = TransactionUtils.newTransaction(peer.getWallet(), to, amount,
                    peer.getUTXOSet());

            if (transaction != null) {
                Message message = new Message(ByteArray.toHexString(transaction.toByteArray()), Type.TRANSACTION);
                peer.getNet().broadcast(message);
            }
        }
    }

    @Override
    public void usage() {
        System.out.println("");
        System.out.println("USAGE [send]:");
        System.out.println("Command: send [receiver] [amount]");
        System.out.println("Description: Make a transaction.");
        System.out.println("");
    }

    @Override
    public boolean check(String[] parameters) {
        if (parameters.length < 2) {
            logger.error("missing parameters");
            return false;
        }

        if (parameters[0].length() != 40) {
            logger.error("address invalid");
            return false;
        }


        long amount = 0;
        try {
            amount = Long.parseLong(parameters[1]);
        } catch (NumberFormatException e) {
            logger.error("amount invalid");
            return false;
        }

        if (amount <= 0) {
            logger.error("amount required a positive number");
            return false;
        }

        return true;
    }
}
