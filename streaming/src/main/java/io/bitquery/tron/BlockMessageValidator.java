package io.bitquery.tron;

import io.bitquery.tron.exception.BlockMessageValidateException;
import lombok.extern.slf4j.Slf4j;
import io.bitquery.protos.TronMessage.BlockMessage;
import io.bitquery.protos.TronMessage.Transaction;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

@Slf4j(topic = "streaming")
public class BlockMessageValidator {
    BlockMessage message;
    long blockNumber;

    public BlockMessageValidator(BlockMessage message) {
        this.message = message;
        this.blockNumber = message.getHeader().getNumber();
    }

    public void validate() throws BlockMessageValidateException {
        logger.info("Validating block protobuf message, Num: {}", message.getHeader().getNumber());

        transactions();
    }

    public void transactions() throws BlockMessageValidateException {
        for (Transaction tx : message.getTransactionsList()) {
            internalTransactionsAndTraces(tx);
            logsAndCaptureStateLogs(tx);
        }
    }

    private void internalTransactionsAndTraces(Transaction tx) throws BlockMessageValidateException {
        String[] internalTxsTypes = {"call", "create", "suicide"};

        long expectedCount = tx.getInternalTransactionsList().stream()
                .filter(x -> ArrayUtils.contains(internalTxsTypes, x.getNote()))
                .count();

        int actualCount = tx.getTrace().getCallsCount();

        if (expectedCount != actualCount) {
            throw new BlockMessageValidateException(
                    String.format(
                            "'Internal Transaction' validation for block %s wasn't passed. Expected: %d, actual: %d. Calls dump: %s",
                            blockNumber,
                            expectedCount,
                            actualCount,
                            tx.getInternalTransactionsList().toString()
                    )
            );
        }
    }

    private void logsAndCaptureStateLogs(Transaction tx) throws BlockMessageValidateException {
        int expectedCount = tx.getLogsCount();
        long actualCount = tx.getTrace().getCaptureStatesList().stream()
                .filter(x -> x.getLog().hasLogHeader())
                .count();

        if (expectedCount != actualCount) {
            throw new BlockMessageValidateException(
                    String.format(
                            "'LOG' validation for block %s wasn't passed. Expected: %d, actual: %d",
                            blockNumber,
                            expectedCount,
                            actualCount
                    )
            );
        }
    }
}
