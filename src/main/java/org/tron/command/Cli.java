package org.tron.command;

import org.tron.peer.Peer;
import java.util.Arrays;
import java.util.Scanner;

public class Cli {
    public Cli() {
    }
    public void run(Peer peer) {
        Scanner in = new Scanner(System.in);

        while (true) {
            String cmd = in.nextLine();
            cmd = cmd.trim();

            String[] cmdArray = cmd.split("\\s+");

            if (cmdArray.length == 0) {
                continue;
            }

            String[] cmdParameters = Arrays.copyOfRange(cmdArray, 1, cmdArray.length);

            switch (cmdArray[0]) {
                case "exit":
                case "quit":
                case "bye":
                    new ExitCommand().execute(peer, cmdParameters);
                    break;
                case "send":
                    new SendCommand().execute(peer, cmdParameters);
                    break;
                case "getbalance":
                    new GetBalanceCommand().execute(peer, cmdParameters);
                    break;
                case "account":
                    new AccountCommand().execute(peer, cmdParameters);
                    break;
                case "printblockchain":
                    new PrintBlockchainCommand().execute(peer, cmdParameters);
                    break;

                case "consensus":
                    new ConsensusCommand().server();
                    break;
                case "getmessage":
                    new ConsensusCommand().getClient(cmdParameters);
                    break;
                case "putmessage":
                    new ConsensusCommand().putClient(cmdParameters);
                    break;
                case "put":
                    new ConsensusCommand().execute(peer, cmdParameters);
                    break;
                case "help":
                default:
                    new HelpCommand().execute(peer, cmdParameters);
                    break;
            }
        }
    }
}
