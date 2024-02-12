package io.bitquery.tron;

import io.bitquery.tron.exception.BlockMessageValidateException;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.streaming.TronMessage;

@Slf4j(topic = "streaming")
public class BlockMessageValidator {
    TronMessage.BlockMessage message;
    long blockNumber;

    public BlockMessageValidator(TronMessage.BlockMessage message) {
        this.message = message;
        this.blockNumber = message.getHeader().getNumber();
    }

     public void validate() throws BlockMessageValidateException {
         logger.info("Validating block protobuf message, Num: {}", message.getHeader().getNumber());

         transactions();
     }

     public void transactions() throws BlockMessageValidateException {
         for (TronMessage.Transaction tx : message.getTransactionsList()) {
             internalTransactionsAndTraces(tx);
             logsAndCaptureStateLogs(tx);
         }
     }

     private void internalTransactionsAndTraces(TronMessage.Transaction tx) throws BlockMessageValidateException {
         int expectedCount = tx.getInternalTransactionsCount();
         int actualCount = tx.getTrace().getCallsCount();

         if (expectedCount != actualCount) {
             throw new BlockMessageValidateException(
                     String.format(
                             "'Internal Transaction' validation for block %s wasn't passed. Expected: %d, actual: %d",
                             blockNumber,
                             expectedCount,
                             actualCount
                     )
             );
         }
     }

     private void logsAndCaptureStateLogs(TronMessage.Transaction tx) throws BlockMessageValidateException {
         int expectedCount = tx.getLogsCount();
         long actualCount = tx.getTrace().getCaptureStatesList().stream()
                 .filter(x -> x.hasLog())
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
