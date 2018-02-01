package org.tron.core.net.node;

import java.util.List;
import org.tron.core.Sha256Hash;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.TransactionMessage;

public interface NodeDelegate {

  void handleBlock(BlockMessage blkMsg);

  void handleTransation(TransactionMessage trxMsg);

  List<Sha256Hash> getBlockIds(List<Sha256Hash> blockChainSummary);

  List<Sha256Hash> getBlockChainSynopsis(Sha256Hash refPoint, int num);

  Message getData(byte[] hash);

  void syncToCli();

  void getBlockNum(byte[] hash);

  void getBlockTime(byte[] hash);

  byte[] getHeadBlockId();

  boolean hasItem(byte[] hash);

}
