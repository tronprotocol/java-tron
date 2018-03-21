package org.tron.core.capsule.utils;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;

import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@Slf4j
public class MerkleTreeTest {
    private static BlockCapsule blockCapsule0 = new BlockCapsule(1, ByteString
            .copyFrom(ByteArray
                    .fromHexString("9938a342238077182498b464ac0292229938a342238077182498b464ac029222")), 1234,
            ByteString.copyFrom("1234567".getBytes()));

    private static BlockCapsule blockCapsule1 = new BlockCapsule(1, ByteString
            .copyFrom(ByteArray
                    .fromHexString("9938a342238077182498b464ac0292229938a342238077182498b464ac029222")), 1234,
            ByteString.copyFrom("1234567".getBytes()));

    private TransactionCapsule transactionCapsule1 = new TransactionCapsule("123", 1L);
    private TransactionCapsule transactionCapsule2 = new TransactionCapsule("124", 2L);
    private TransactionCapsule transactionCapsule3 = new TransactionCapsule("125", 2L);

    @Test
    public void testMerkleTreeTest() {
        Sha256Hash hash1 = getBeforeZeroHash();
        MerkleTree tree = MerkleTree.getInstance().createTree(getZeroIds());

        logger.info("Transaction[X] Compare :");
        logger.info("left: {}", hash1);
        logger.info("right: {}", tree.getRoot().getHash());

        assertEquals(hash1, tree.getRoot().getHash());

        Sha256Hash hash2 = getBeforeTxHash();
        tree.createTree(getTxIds2(blockCapsule1));

        logger.info("Transaction[O] Compare :");
        logger.info("left: {}", hash2);
        logger.info("right: {}", tree.getRoot().getHash());

        assertEquals(hash2, tree.getRoot().getHash());
    }

    private Sha256Hash getBeforeHash(Vector<Sha256Hash> ids) {
        int hashNum = ids.size();

        while (hashNum > 1) {
            int max = hashNum - (hashNum & 1);
            int k = 0;
            for (int i = 0; i < max; i += 2) {
                ids.set(k++, Sha256Hash.of((ids.get(i).getByteString()
                        .concat(ids.get(i + 1).getByteString()))
                        .toByteArray()));
            }

            if (hashNum % 2 == 1) {
                ids.set(k++, ids.get(max));
            }
            hashNum = k;
        }

        return ids.firstElement();
    }

    private Sha256Hash getBeforeZeroHash() {
        return getBeforeHash(getZeroIds());
    }

    private Sha256Hash getBeforeTxHash() {
        return getBeforeHash(getTxIds1(blockCapsule0));
    }

    private Vector<Sha256Hash> getZeroIds() {
        Vector<Sha256Hash> ids = new Vector<>();
        ids.add(Sha256Hash.ZERO_HASH);
        return ids;
    }

    private Vector<Sha256Hash> getTxIds1(BlockCapsule blockCapsule) {
        return getSha256Hashes(blockCapsule);
    }

    private Vector<Sha256Hash> getTxIds2(BlockCapsule blockCapsule) {
        return getSha256Hashes(blockCapsule);
    }

    private Vector<Sha256Hash> getSha256Hashes(BlockCapsule blockCapsule) {
        blockCapsule.addTransaction(transactionCapsule1);
        blockCapsule.addTransaction(transactionCapsule2);
        blockCapsule.addTransaction(transactionCapsule3);

        List<Protocol.Transaction> transactionList = blockCapsule.getInstance().getTransactionsList();

        return getSha256Hashes(transactionList);
    }

    private Vector<Sha256Hash> getSha256Hashes(List<Protocol.Transaction> transactionList) {
        return transactionList.stream()
                .map(TransactionCapsule::new)
                .map(TransactionCapsule::getHash)
                .collect(Collectors.toCollection(Vector::new));
    }
}