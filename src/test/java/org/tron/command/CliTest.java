package org.tron.command;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.peer.Peer;

public class CliTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testCli() {
        Cli cli = new Cli();
        cli.run(Peer.getInstance(Peer.PEER_NORMAL));
    }
}
