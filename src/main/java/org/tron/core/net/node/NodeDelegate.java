package org.tron.core.net.node;

import java.util.ArrayList;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.TransactionMessage;
import org.tron.protos.Protocal.Inventory;

public interface NodeDelegate {

  void handleBlock(BlockMessage blkMsg);

  void handleTransation(TransactionMessage trxMsg);

  boolean isIncludedBlock(byte[] hash);

  Inventory getBlockIds(Inventory inv);


  ArrayList<byte[]> getBlockChainSynopsis(byte[] refPoint, int num);

  Message getData(byte[] hash);

  void syncToCli();

  void getBlockNum(byte[] hash);

  void getBlockTime(byte[] hash);

  byte[] getHeadBlockId();

  boolean hasItem(byte[] hash);

}
