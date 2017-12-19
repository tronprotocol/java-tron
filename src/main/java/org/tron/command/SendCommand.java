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
    private static final Logger logger = LoggerFactory.getLogger("command");

    public SendCommand() {
    }

    @Override
    public void execute(Peer peer, String[] parameters) {
        if (check(parameters)) {
            String to = parameters[0];
            long amount = Long.valueOf(parameters[1]);
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
        System.out.println("Command: send [receiver] [mount]");
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
            amount = Long.valueOf(parameters[1]);
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
