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
package org.tron.core;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.protos.core.TronBlock.Block;
import org.tron.protos.core.TronTXOutputs;
import org.tron.protos.core.TronTransaction.Transaction;
import org.tron.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.utils.ByteArray;
import org.tron.wallet.Wallet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.tron.core.Blockchain.dbExists;
import static org.tron.utils.ByteArray.toHexString;

@Ignore
public class BlockchainTest {
    private static final Logger logger = LoggerFactory.getLogger("Test");
    private static Blockchain blockchain;

    @BeforeClass
    public static void init() {
       blockchain = new Blockchain
               ("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b85","server");
    }

    @AfterClass
    public static void teardown() {
        blockchain.getBlockDB().closeDB();
    }

    @Test
    public void testBlockchain() {
        Blockchain blockchain = new Blockchain
                ("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b85","server");
        logger.info("test blockchain: lashHash = {}, currentHash = {}",
                ByteArray.toHexString(blockchain.getLastHash()), ByteArray
                        .toHexString(blockchain.getCurrentHash()));
    }

    @Test
    public void testBlockchainNew() {
        logger.info("test blockchain new: lastHash = {}", ByteArray
                .toHexString(blockchain.getLastHash()));

        byte[] blockBytes = blockchain.getBlockDB().getData(blockchain.getLastHash());

        try {
            Block block = Block.parseFrom(blockBytes);

            for (Transaction transaction : block.getTransactionsList()) {
                logger.info("transaction id = {}", ByteArray.toHexString
                        (transaction.getId().toByteArray()));
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIterator() {

        Blockchain blockchain = new Blockchain("","server");
        Block info = null;
        BlockchainIterator bi = new BlockchainIterator(blockchain);
        while (bi.hasNext()) {
            info = (Block) bi.next();
            logger.info("blockParentHash:{},number:{}", toHexString(info
                    .getBlockHeader()
                    .getParentHash().toByteArray()), info.getBlockHeader()
                    .getNumber());
        }
    }

    @Test
    public void testFindTransaction() {
        Transaction transaction = blockchain.findTransaction(ByteString
                .copyFrom(ByteArray.fromHexString
                        ("15f3988aa8d56eab3bfca45144bad77fc60acce50437a0a9d794a03a83c15c5e")));
        logger.info("{}", TransactionUtils.toPrintString(transaction));
    }

    @Test
    public void testDBExists() {
        logger.info("test dbStore exists: {}", dbExists());
    }

    @Test
    public void testFindUTXO() {
        long testAmount = 10;
        Blockchain blockchain = new Blockchain("fd0f3c8ab4877f0fd96cd156b0ad42ea7aa82c31","server");
        Wallet wallet = new Wallet();
        wallet.init();
        SpendableOutputs spendableOutputs = new SpendableOutputs();
        spendableOutputs.setAmount(testAmount + 1);
        spendableOutputs.setUnspentOutputs(new HashMap<>());
        UTXOSet mockUtxoSet = Mockito.mock(UTXOSet.class);
        Mockito.when(mockUtxoSet.findSpendableOutputs(wallet.getEcKey().getPubKey(), testAmount)
        ).thenReturn(spendableOutputs);
        Mockito.when(mockUtxoSet.getBlockchain()).thenReturn(blockchain);

        Transaction transaction = TransactionUtils.newTransaction(wallet,
                "fd0f3c8ab4877f0fd96cd156b0ad42ea7aa82c31", testAmount, mockUtxoSet);
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);
        blockchain.addBlock(BlockUtils.newBlock(transactions, ByteString
                .copyFrom(new byte[]{1}), ByteString
                .copyFrom(new byte[]{1}), 1));
        HashMap<String, TronTXOutputs.TXOutputs> utxo = blockchain.findUTXO();
    }

    @Test
    public void testAddBlockToChain() {
        ByteString parentHash = ByteString.copyFrom(ByteArray.fromHexString
                ("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b85"));
        ByteString difficulty = ByteString.copyFrom(ByteArray.fromHexString
                ("2001"));

        Wallet wallet = new Wallet();
        wallet.init();

        Block block = BlockUtils.newBlock(null, parentHash,
                difficulty, 0);
        LevelDbDataSourceImpl levelDbDataSource = new LevelDbDataSourceImpl
                ("blockStore_test");
        levelDbDataSource.initDB();
        String lastHash = "lastHash";
        byte[] key = lastHash.getBytes();
        String value = "090383489592535";
        byte[] values = value.getBytes();
        levelDbDataSource.putData(key, values);

        blockchain.addBlock(block);
        levelDbDataSource.closeDB();
    }
}
