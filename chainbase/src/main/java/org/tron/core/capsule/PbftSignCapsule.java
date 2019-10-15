package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.DataSign;

@Slf4j(topic = "pbft")
public class PbftSignCapsule implements ProtoCapsule<DataSign> {

  private DataSign dataSign;

  public PbftSignCapsule(byte[] data) {
    try {
      dataSign = DataSign.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.error("", e);
    }
  }

  public PbftSignCapsule(ByteString data, List<ByteString> signList) {
    DataSign.Builder builder = DataSign.newBuilder();
    builder.setData(data).addAllSign(signList.stream().collect(Collectors.toList()));
    dataSign = builder.build();
  }

  @Override
  public byte[] getData() {
    return dataSign.toByteArray();
  }

  @Override
  public DataSign getInstance() {
    return dataSign;
  }
}
