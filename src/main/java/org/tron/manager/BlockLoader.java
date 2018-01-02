/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.config.SystemProperties;
import org.tron.core.TransactionUtils;
import org.tron.core.TronBlockChainImpl;
import org.tron.protos.core.TronBlock;
import org.tron.protos.core.TronTransaction;
import org.tron.utils.ExecutorPipeline;

import java.io.FileInputStream;
import java.util.Scanner;
import java.util.function.Function;

public class BlockLoader {

    private static final Logger logger = LoggerFactory.getLogger("BlockLoader");

    @Autowired
    private TronBlockChainImpl blockchain;

    @Autowired
    SystemProperties config;

    Scanner scanner = null;

    ExecutorPipeline<TronBlock.Block, TronBlock.Block> exce1;
    ExecutorPipeline<TronBlock.Block, ?> exce2;

    public void loadBlocks() {
        exce1 = new ExecutorPipeline(8, 1000, true, (Function<TronBlock.Block, TronBlock.Block>) block -> {
            if (block.getBlockHeader().getNumber() >= blockchain
                    .getBlockStore().getBestBlock().getBlockHeader()
                    .getNumber()) {
                for (TronTransaction.Transaction tx : block
                        .getTransactionsList()) {
                    TransactionUtils.getSender(tx);
                }
            }
            return block;
        }, throwable -> logger.error("Unhandled exception: ", throwable));

        exce2 = exce1.add(1, 1000, block -> {
            try {
                blockWork(block);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        String fileSrc = config.blocksLoader();

        try {
            final String blocksFormat = config.getConfig().hasPath("blocks" +
                    ".format") ? config.getConfig().getString
                    ("blocks.format") : null;
            System.out.println("Loading blocks: " + fileSrc + ", format: " +
                    blocksFormat);

            FileInputStream inputStream = new FileInputStream(fileSrc);

            scanner = new Scanner(inputStream, "UTF-8");

            while (scanner.hasNext()) {
                byte[] blockBytes = Hex.decode(scanner.nextLine());
                TronBlock.Block block = TronBlock.Block.parseFrom(blockBytes);

                exce1.push(block);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private void blockWork(TronBlock.Block block) {
        if (block.getBlockHeader().getNumber() >= blockchain.getBlockStore()
                .getBestBlock().getBlockHeader().getNumber()
                || blockchain.getBlockStore().getBlockByHash(block
                .getBlockHeader().getHash().toByteArray()) == null) {
            if (block.getBlockHeader().getNumber() > 0) {
                throw new RuntimeException();
            }
        }
    }
}
