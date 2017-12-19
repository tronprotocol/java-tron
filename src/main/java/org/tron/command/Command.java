package org.tron.command;

import org.tron.peer.Peer;

public abstract class Command {
    public abstract void execute(Peer peer, String[] parameters);

    public abstract void usage();

    public abstract boolean check(String[] parameters);
}
