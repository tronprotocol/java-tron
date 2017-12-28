package org.tron.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.consensus.client.Client;
import org.tron.consensus.server.Server;
import org.tron.core.TransactionUtils;
import org.tron.overlay.message.Message;
import org.tron.overlay.message.Type;
import org.tron.peer.Peer;
import org.tron.protos.core.TronTransaction;
import org.tron.utils.ByteArray;

public class ConsensusCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger
            ("consensus-command");

    public void server() {
        Server.serverRun();
    }

    public void putClient(String[] args) {
        Client.putMessage(args);
    }

    public void getClient(String[] args) {
        Client.getMessage1(args[0]);
    }

    public void usage() {
        System.out.println("");
        System.out.println("consensus server");
        System.out.println("Command: consensus");
        System.out.println("Description: Create a server.");
        System.out.println("");

        System.out.println("");
        System.out.println("get Message");
        System.out.println("Command: getmessage [key]");
        System.out.println("Description: Get consensus Message");
        System.out.println("");

        System.out.println("");
        System.out.println("put Message");
        System.out.println("Command: putmessage [key] [value]");
        System.out.println("Description: Put a consensus Message");
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
                System.out.println(message);
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
