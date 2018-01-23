package org.tron.core.net.node;

import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.TransationMessage;

import java.util.ArrayList;

public interface NodeDelegate {

    void reset();

    void start();

    void stop();

    void handleBlock(BlockMessage blkMsg);

    void handleTransation(TransationMessage trxMsg);

    void handleMsg(Message msg);

    boolean isIncludedBlock(int blkId);

    ArrayList<Integer> getBlockIds(ArrayList<Integer> blockChainSynopsis);

    ArrayList<Integer> getBlockChainSynopsis(int refPoint, int num);

    void sync();

    void getBlockNum(int blkId);

    void getBlockTime(int blkId);

    void getHeadBlockId();

}
