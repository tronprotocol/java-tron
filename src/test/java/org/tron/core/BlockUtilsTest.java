package org.tron.core;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.protos.core.TronBlock.Block;
import org.tron.protos.core.TronTransaction.Transaction;
import org.tron.utils.ByteArray;

import static org.tron.core.Blockchain.genesisCoinbaseData;

public class BlockUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testNewBlock() {
        ByteString parentHash = ByteString.copyFrom(ByteArray.fromHexString
                ("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b85"));
        ByteString difficulty = ByteString.copyFrom(ByteArray.fromHexString
                ("2001"));
        Block block = BlockUtils.newBlock(null, parentHash, difficulty, 0);

        logger.info("test new block: {}", BlockUtils.toPrintString(block));
    }

    @Test
    public void testNewGenesisBlock() {
        Transaction coinbase = TransactionUtils.newCoinbaseTransaction
                ("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b85", genesisCoinbaseData, 0);
        Block genesisBlock = BlockUtils.newGenesisBlock(coinbase);

        logger.info("test new genesis block: {}", BlockUtils.toPrintString
                (genesisBlock));
    }

    @Test
    public void testPrepareData() {
        Transaction coinbase = TransactionUtils.newCoinbaseTransaction
                ("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b85", genesisCoinbaseData, 0);
        logger.info("test prepare data: {}",
                "12580a2015f3988aa8d56eab3bfca45144bad77fc60acce50437a0a9d794a03a83c15c5e120e10ffffffffffffffffff012201001a24080a12200304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b8532022001".equals(ByteArray.toHexString(BlockUtils.prepareData(BlockUtils.newGenesisBlock(coinbase)))));
    }

    @Test
    public void testIsValidate() {
        Transaction coinbase = TransactionUtils.newCoinbaseTransaction
                ("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b85", genesisCoinbaseData, 0);
        Block genesisBlock = BlockUtils.newGenesisBlock(coinbase);
        logger.info("nonce: {}", ByteArray.toHexString(genesisBlock.getBlockHeader().getNonce
                ().toByteArray()));
        logger.info("test is validate: {}", BlockUtils.isValidate
                (genesisBlock));
    }

    @Test
    public void testToPrintString() {
        Transaction coinbase = TransactionUtils.newCoinbaseTransaction
                ("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b85", genesisCoinbaseData, 0);
        logger.info("test to print string: {}", BlockUtils.toPrintString
                (BlockUtils.newGenesisBlock(coinbase)));
    }

    @Test
    public void testGetMineValue() {
        Transaction coinbase = TransactionUtils.newCoinbaseTransaction
                ("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b85", genesisCoinbaseData, 0);
        logger.info("test get mine value: {}", ByteArray.toHexString
                (BlockUtils.getMineValue(BlockUtils.newGenesisBlock(coinbase)
                )));
    }

    @Test
    public void testGetPowBoundary() {
        Transaction coinbase = TransactionUtils.newCoinbaseTransaction
                ("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b85", genesisCoinbaseData, 0);
        logger.info("test get pow boundary: {}", ByteArray.toHexString
                (BlockUtils.getPowBoundary(BlockUtils.newGenesisBlock
                        (coinbase))));
    }

    @Test
    public void testGetIncreaseNumber() {
        logger.info("test get increase number: {}", BlockUtils
                .getIncreaseNumber(new Blockchain()));
    }
}
