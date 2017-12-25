package org.tron.command;

import org.tron.consensus.client.Client;
import org.tron.consensus.server.Server;

public class ConsensusCommand {

    public ConsensusCommand() {

    }

    public void server() {
        Server.serverRun();
    }

    public void putClient(String[] args) {
        Client.putMessage(args);
    }

    public void getClient(String[] args) {
        Client.getMessage(args[0]);
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

}
