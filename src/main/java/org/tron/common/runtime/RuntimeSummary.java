package org.tron.common.runtime;

import org.springframework.util.Assert;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.protos.Protocol.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.*;

public class RuntimeSummary {

    private Transaction tx;
    private BigInteger value = BigInteger.ZERO;

    private List<DataWord> deletedAccounts = emptyList();
    private List<InternalTransaction> internalTransactions = emptyList();
    private Map<DataWord, DataWord> storageDiff = emptyMap();
    //private TransactionTouchedStorage touchedStorage = new TransactionTouchedStorage();


    private byte[] result;
    private boolean failed;
    private byte[] data;

    public RuntimeSummary(Transaction transaction) {
        this.tx = transaction;
        //this.value = toBI(transaction.getValue());
    }

    public RuntimeSummary(byte[] data) {
        this.data = data;
    }

    /*
    public void rlpParse() {
        if (parsed) return;

        RLPList decodedTxList = RLP.decode2(rlpEncoded);
        RLPList summary = (RLPList) decodedTxList.get(0);

        this.tx = new Transaction(summary.get(0).getRLPData());
        this.value = decodeBigInteger(summary.get(1).getRLPData());
        this.deletedAccounts = decodeDeletedAccounts((RLPList) summary.get(7));
        this.internalTransactions = decodeInternalTransactions((RLPList) summary.get(8));
        //this.touchedStorage = decodeTouchedStorage(summary.get(9));
        this.result = summary.get(10).getRLPData();
        this.logs = decodeLogs((RLPList) summary.get(11));
        byte[] failed = summary.get(12).getRLPData();
        this.failed = isNotEmpty(failed) && RLP.decodeInt(failed, 0) == 1;
    }

    private static BigInteger decodeBigInteger(byte[] encoded) {
        return ByteUtil.bytesToBigInteger(encoded);
    }


    public byte[] getEncoded() {
        if (rlpEncoded != null) return rlpEncoded;


        this.rlpEncoded = RLP.encodeList(
                this.tx.getEncoded(),
                RLP.encodeBigInteger(this.value),
                RLP.encodeBigInteger(this.gasPrice),
                RLP.encodeBigInteger(this.gasLimit),
                RLP.encodeBigInteger(this.gasUsed),
                RLP.encodeBigInteger(this.gasLeftover),
                RLP.encodeBigInteger(this.gasRefund),
                encodeDeletedAccounts(this.deletedAccounts),
                encodeInternalTransactions(this.internalTransactions),
                encodeTouchedStorage(this.touchedStorage),
                RLP.encodeElement(this.result),
                encodeLogs(this.logs),
                RLP.encodeInt(this.failed ? 1 : 0)
        );

        return rlpEncoded;
    }

    public static byte[] encodeTouchedStorage(TransactionTouchedStorage touchedStorage) {
        Collection<TransactionTouchedStorage.Entry> entries = touchedStorage.getEntries();
        byte[][] result = new byte[entries.size()][];

        int i = 0;
        for (TransactionTouchedStorage.Entry entry : entries) {
            byte[] key = RLP.encodeElement(entry.getKey().getData());
            byte[] value = RLP.encodeElement(entry.getValue().getData());
            byte[] changed = RLP.encodeInt(entry.isChanged() ? 1 : 0);

            result[i++] = RLP.encodeList(key, value, changed);
        }

        return RLP.encodeList(result);
    }

    public static TransactionTouchedStorage decodeTouchedStorage(RLPElement encoded) {
        TransactionTouchedStorage result = new TransactionTouchedStorage();

        for (RLPElement entry : (RLPList) encoded) {
            RLPList asList = (RLPList) entry;

            DataWord key = new DataWord(asList.get(0).getRLPData());
            DataWord value = new DataWord(asList.get(1).getRLPData());
            byte[] changedBytes = asList.get(2).getRLPData();
            boolean changed = isNotEmpty(changedBytes) && RLP.decodeInt(changedBytes, 0) == 1;

            result.add(new TransactionTouchedStorage.Entry(key, value, changed));
        }

        return result;
    }
    */

    /*
    private static List<LogInfo> decodeLogs(RLPList logs) {
        ArrayList<LogInfo> result = new ArrayList<>();
        for (RLPElement log : logs) {
            result.add(new LogInfo(log.getRLPData()));
        }
        return result;
    }

    private static byte[] encodeLogs(List<LogInfo> logs) {
        byte[][] result = new byte[logs.size()][];
        for (int i = 0; i < logs.size(); i++) {
            LogInfo log = logs.get(i);
            result[i] = log.getEncoded();
        }

        return RLP.encodeList(result);
    }

    private static byte[] encodeStorageDiff(Map<DataWord, DataWord> storageDiff) {
        byte[][] result = new byte[storageDiff.size()][];
        int i = 0;
        for (Map.Entry<DataWord, DataWord> entry : storageDiff.entrySet()) {
            byte[] key = RLP.encodeElement(entry.getKey().getData());
            byte[] value = RLP.encodeElement(entry.getValue().getData());
            result[i++] = RLP.encodeList(key, value);
        }
        return RLP.encodeList(result);
    }

    private static Map<DataWord, DataWord> decodeStorageDiff(RLPList storageDiff) {
        Map<DataWord, DataWord> result = new HashMap<>();
        for (RLPElement entry : storageDiff) {
            DataWord key = new DataWord(((RLPList) entry).get(0).getRLPData());
            DataWord value = new DataWord(((RLPList) entry).get(1).getRLPData());
            result.put(key, value);
        }
        return result;
    }

    private static byte[] encodeInternalTransactions(List<InternalTransaction> internalTransactions) {
        byte[][] result = new byte[internalTransactions.size()][];
        for (int i = 0; i < internalTransactions.size(); i++) {
            InternalTransaction transaction = internalTransactions.get(i);
            result[i] = transaction.getEncoded();
        }

        return RLP.encodeList(result);
    }

    private static List<InternalTransaction> decodeInternalTransactions(RLPList internalTransactions) {
        List<InternalTransaction> result = new ArrayList<>();
        for (RLPElement internalTransaction : internalTransactions) {
            result.add(new InternalTransaction(internalTransaction.getRLPData()));
        }
        return result;
    }

    private static byte[] encodeDeletedAccounts(List<DataWord> deletedAccounts) {
        byte[][] result = new byte[deletedAccounts.size()][];
        for (int i = 0; i < deletedAccounts.size(); i++) {
            DataWord deletedAccount = deletedAccounts.get(i);
            result[i] = RLP.encodeElement(deletedAccount.getData());

        }
        return RLP.encodeList(result);
    }

    private static List<DataWord> decodeDeletedAccounts(RLPList deletedAccounts) {
        List<DataWord> result = new ArrayList<>();
        for (RLPElement deletedAccount : deletedAccounts) {
            result.add(new DataWord(deletedAccount.getRLPData()));
        }
        return result;
    }
    */

    public Transaction getTransaction() {
        //if (!parsed) rlpParse();
        return tx;
    }

    /*
    public byte[] getTransactionHash() {
        return getTransaction().getRawDataOrBuilder().;
    }
    */

    public BigInteger getValue() {
        //if (!parsed) rlpParse();
        return value;
    }

    public List<DataWord> getDeletedAccounts() {
        //if (!parsed) rlpParse();
        return deletedAccounts;
    }

    public List<InternalTransaction> getInternalTransactions() {
        //if (!parsed) rlpParse();
        return internalTransactions;
    }

    @Deprecated
    /* Use getTouchedStorage().getAll() instead */
    public Map<DataWord, DataWord> getStorageDiff() {
        //if (!parsed) rlpParse();
        return storageDiff;
    }

    public boolean isFailed() {
        //if (!parsed) rlpParse();
        return failed;
    }

    public byte[] getResult() {
        //if (!parsed) rlpParse();
        return result;
    }

    /*
    public TransactionTouchedStorage getTouchedStorage() {
        return touchedStorage;
    }
    */

    public static Builder builderFor(Transaction transaction) {
        return new Builder(transaction);
    }

    public static class Builder {

        private final RuntimeSummary summary;

        Builder(Transaction transaction) {
            Assert.notNull(transaction, "Cannot build TransactionExecutionSummary for null transaction.");
            summary = new RuntimeSummary(transaction);
        }

        public Builder internalTransactions(List<InternalTransaction> internalTransactions) {
            summary.internalTransactions = unmodifiableList(internalTransactions);
            return this;
        }

        public Builder deletedAccounts(Set<DataWord> deletedAccounts) {
            summary.deletedAccounts = new ArrayList<>();
            for (DataWord account : deletedAccounts) {
                summary.deletedAccounts.add(account);
            }
            return this;
        }

        /*
        public Builder touchedStorage(Map<DataWord, DataWord> touched, Map<DataWord, DataWord> changed) {
            summary.touchedStorage.addReading(touched);
            summary.touchedStorage.addWriting(changed);
            return this;
        }
        */

        public Builder markAsFailed() {
            summary.failed = true;
            return this;
        }


        public Builder result(byte[] result) {
            summary.result = result;
            return this;
        }

        public RuntimeSummary build() {
            if (summary.failed) {
                for (InternalTransaction transaction : summary.internalTransactions) {
                    transaction.reject();
                }
            }
            return summary;
        }
    }
}
