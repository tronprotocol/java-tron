package org.tron.command;

import org.tron.peer.Peer;

public class ExitCommand extends Command {
    public ExitCommand() {
    }

    @Override
    public void execute(Peer peer, String[] parameters) {
        System.exit(0);
    }

    @Override
    public void usage() {
        System.out.println("");
        System.out.println("USAGE [exit]:");
        System.out.println("Command: exit | quit | bye");
        System.out.println("Description: Exit the program.");
        System.out.println("");
    }

    @Override
    public boolean check(String[] parameters) {
        return true;
    }
}
