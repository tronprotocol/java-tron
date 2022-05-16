package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.OraclePrevote;

@Slf4j(topic = "capsule")
public class OraclePrevoteCapsule implements ProtoCapsule<Protocol.OraclePrevote> {
  private OraclePrevote prevote;

  public OraclePrevoteCapsule(long blockNum, byte[] hash) {
    this.prevote = OraclePrevote.newBuilder()
            .setBlockNum(blockNum)
            .setHash(ByteString.copyFrom(hash))
            .build();
  }

  public OraclePrevoteCapsule(final byte[] data) {
    try {
      this.prevote = OraclePrevote.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  @Override
  public byte[] getData() {
    return this.prevote.toByteArray();
  }

  @Override
  public OraclePrevote getInstance() {
    return this.prevote;
  }
}
