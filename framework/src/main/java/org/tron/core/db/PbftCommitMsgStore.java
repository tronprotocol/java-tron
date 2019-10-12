package org.tron.core.db;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.core.capsule.PbftCommitMsgCapsule;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Protocol.PbftMessage;

@Slf4j(topic = "DB")
@Component
public class PbftCommitMsgStore extends TronDatabase<PbftCommitMsgCapsule> {

  private static final byte[] SR_LIST_KEY = "current_sr_list".getBytes();

  @Autowired
  private PbftCommitMsgStore(@Value("pbftcommit") String dbName) {
    super(dbName);
  }

  @Override
  public PbftCommitMsgCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new PbftCommitMsgCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }

  @Override
  public void put(byte[] key, PbftCommitMsgCapsule item) {
    dbSource.putData(key, item.getData());
  }

  @Override
  public void delete(byte[] key) {
    dbSource.deleteData(key);
  }

  public synchronized void put(PbftBaseMessage pbftBaseMessage) {
    byte[] key = buildKey(pbftBaseMessage);
    PbftCommitMsgCapsule pbftCommitMsgCapsule = get(key);
    List<PbftMessage> pbftMessageList = new ArrayList<>();
    pbftMessageList.add(pbftBaseMessage.getPbftMessage());
    if (pbftCommitMsgCapsule != null && pbftBaseMessage.getBlockNum() == pbftCommitMsgCapsule
        .getInstance().getBlockNum()) {
      pbftMessageList.addAll(pbftCommitMsgCapsule.getInstance().getPbftMessageList());
    }
    put(key, new PbftCommitMsgCapsule(pbftBaseMessage.getBlockNum(), pbftMessageList));
  }

  @Override
  public void close() {
    super.close();
  }

  private byte[] buildKey(PbftBaseMessage message) {
    if (message.getType() == MessageTypes.PBFT_SR_MSG) {
      return SR_LIST_KEY;
    }
    String key = String.valueOf(message.getBlockNum()) + "_" + message.getType().asByte();
    return key.getBytes();
  }
}
