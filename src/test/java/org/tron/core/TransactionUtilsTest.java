package org.tron.core;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.protos.core.TronTransaction.Transaction;
import org.tron.utils.ByteArray;

public class TransactionUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testNewCoinbaseTransaction() {
        Transaction coinbaseTransaction = TransactionUtils
                .newCoinbaseTransaction("12", "");

        logger.info("test new coinbase transaction: {}", coinbaseTransaction);
    }

    @Test
    public void testGetHash() {
        Transaction coinbaseTransaction = TransactionUtils
                .newCoinbaseTransaction("12", "");

        logger.info("test getData hash: {}", ByteArray.toHexString
                (TransactionUtils.getHash(coinbaseTransaction)));
    }

    @Test
    public void testToPrintString() {
        Transaction coinbaseTransaction = TransactionUtils
                .newCoinbaseTransaction("12", "");

        logger.info("test to print string: {}", TransactionUtils
                .toPrintString(coinbaseTransaction));
    }

    @Test
    public void testIsCoinbaseTransaction() {
        Transaction coinbaseTransaction = TransactionUtils
                .newCoinbaseTransaction("12", "");

        logger.info("test is coinbase transaction: {}", TransactionUtils
                .isCoinbaseTransaction(coinbaseTransaction));
    }

    @Test
    public void testParseTransaction() {
        String transactionData =
                "12650a202dbb0466bb1bc2f4b1432e62307160084c14eeab2b093f11969db06c07f3012f22410417017022a990673f2291d73a45621dc4bc754e3313f5a9cea1421b9ea0133d92a3a029c1be7d947b709195ea02370e05712cea4a699edb6efe8fedfe18eb4fcb1a1808011214fd0f3c8ab4877f0fd96cd156b0ad42ea7aa82c311a1808091214fd0f3c8ab4877f0fd96cd156b0ad42ea7aa82c31";
        try {
            Transaction transaction = Transaction.parseFrom(ByteArray.fromHexString(transactionData));
            System.out.println();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
