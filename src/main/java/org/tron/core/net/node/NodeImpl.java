package org.tron.core.net.node;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.BlockUtils;
import org.tron.core.TransactionUtils;
import org.tron.core.db.BlockStore;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.TransationMessage;
import org.tron.protos.core.TronBlock;
import org.tron.protos.core.TronTransaction;

import java.util.ArrayList;

public class NodeImpl implements NodeDelegate {

    private static final Logger logger = LoggerFactory.getLogger("NodeImpl");
    private Node p2pNode;
    private BlockStore blockdb;

    // set seeds
    @java.lang.Override
    public void reset() {
        p2pNode = new Node();
        p2pNode.setNodeDelegate(this);
    }

    @java.lang.Override
    public void start() {
        // init database
        logger.info("init database");
        blockdb = new BlockStore();
        blockdb.initBlockDbSource("database-test", "block");
        blockdb.initUnspendDbSource("database-test", "trx");

        logger.info("reset p2p network");
        reset();

    }

    @java.lang.Override
    public void stop() {

    }

    @java.lang.Override
    public void handleBlock(BlockMessage blkMsg) {
        TronBlock.Block block = null;
        try {
            block = TronBlock.Block.parseFrom(blkMsg.getData());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        System.out.println("handle block: ");
        System.out.println(BlockUtils.toPrintString(block));
    }

    @java.lang.Override
    public void handleTransation(TransationMessage trxMsg) {
        TronTransaction.Transaction transaction = null;
        try {
            transaction = TronTransaction.Transaction.parseFrom(trxMsg.getData());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        System.out.println("handle transaction: ");
        System.out.println(TransactionUtils.toPrintString(transaction));
    }

    @java.lang.Override
    public void handleMsg(Message msg) {

    }

    @java.lang.Override
    public boolean isIncludedBlock(int blkId) {
        return false;
    }

    @java.lang.Override
    public ArrayList<Integer> getBlockIds(ArrayList<Integer> blockChainSynopsis) {
        return null;
    }

    @java.lang.Override
    public ArrayList<Integer> getBlockChainSynopsis(int refPoint, int num) {
        return null;
    }

    @java.lang.Override
    public void sync() {

    }

    @java.lang.Override
    public void getBlockNum(int blkId) {

    }

    @java.lang.Override
    public void getBlockTime(int blkId) {

    }

    @java.lang.Override
    public void getHeadBlockId() {

    }

    public Node getP2pNode() {
        return p2pNode;
    }

    public void setP2pNode(Node p2pNode) {
        this.p2pNode = p2pNode;
    }

    public BlockStore getBlockdb() {
        return blockdb;
    }

    public void setBlockdb(BlockStore blockdb) {
        this.blockdb = blockdb;
    }
}
