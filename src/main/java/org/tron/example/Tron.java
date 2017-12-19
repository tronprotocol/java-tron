package org.tron.example;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.tron.command.Cli;
import org.tron.peer.Peer;
import org.tron.peer.PeerType;

public class Tron {
    @Parameter(names = {"--type", "-t"}, validateWith = PeerType.class)
    private String type = "normal";

    private static Peer peer;

    public static void main(String[] args) {
        Tron tron = new Tron();
        JCommander.newBuilder()
                .addObject(tron)
                .build()
                .parse(args);
        tron.run();
    }

    public void run() {
        peer = Peer.getInstance(type);
        Cli cli = new Cli();
        cli.run(peer);
    }

    public static Peer getPeer() {
        return peer;
    }
}
