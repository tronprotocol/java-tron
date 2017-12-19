package org.tron.command;

import org.tron.core.BlockUtils;
import org.tron.core.Blockchain;
import org.tron.core.BlockchainIterator;
import org.tron.peer.Peer;
import org.tron.protos.core.TronBlock;

public class PrintBlockchainCommand extends Command {
    public PrintBlockchainCommand() {
    }

    @Override
    public void execute(Peer peer, String[] parameters) {
        Blockchain blockchian = peer.getUTXOSet().getBlockchain();
        BlockchainIterator bi = new BlockchainIterator(blockchian);
        while (bi.hasNext()) {
            TronBlock.Block block = (TronBlock.Block) bi.next();
            System.out.println(BlockUtils.toPrintString(block));
        }
    }

    @Override
    public void usage() {
        System.out.println("");
        System.out.println("USAGE [printblockchain]:");
        System.out.println("Command: printblockchain");
        System.out.println("Description: Print Blockchain.");
        System.out.println("");
    }

    @Override
    public boolean check(String[] parameters) {
        return true;
    }
}
