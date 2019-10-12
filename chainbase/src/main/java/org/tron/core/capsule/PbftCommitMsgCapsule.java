package org.tron.core.capsule;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.PbftMessage;
import org.tron.protos.Protocol.PbftMessageList;

@Slf4j(topic = "pbft")
public class PbftCommitMsgCapsule implements ProtoCapsule<PbftMessageList> {

  private PbftMessageList pbftMessageList;

  public PbftCommitMsgCapsule(byte[] data) {
    try {
      pbftMessageList = PbftMessageList.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.error("", e);
    }
  }

  public PbftCommitMsgCapsule(long blockNum, List<PbftMessage> pbftMessages) {
    PbftMessageList.Builder builder = PbftMessageList.newBuilder();
    builder.setBlockNum(blockNum).addAllPbftMessage(pbftMessages);
    pbftMessageList = builder.build();
  }

  @Override
  public byte[] getData() {
    return pbftMessageList.toByteArray();
  }

  @Override
  public PbftMessageList getInstance() {
    return pbftMessageList;
  }
}
