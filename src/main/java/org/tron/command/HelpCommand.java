package org.tron.command;

import org.tron.peer.Peer;

public class HelpCommand extends Command {
    public HelpCommand() {
    }

    @Override
    public void execute(Peer peer, String[] parameters) {
        new ExitCommand().usage();
        new SendCommand().usage();
        new GetBalanceCommand().usage();
        new AccountCommand().usage();
        new PrintBlockchainCommand().usage();
        new ConsensusCommand().usage();
    }

    @Override
    public void usage() {
        System.out.printf("USAGE [help]");
        System.out.println("help");
    }

    @Override
    public boolean check(String[] parameters) {
        return true;
    }
}
