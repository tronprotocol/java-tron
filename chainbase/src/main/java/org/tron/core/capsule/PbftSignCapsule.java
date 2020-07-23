package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.PBFTCommitResult;

@Slf4j(topic = "pbft")
public class PbftSignCapsule implements ProtoCapsule<PBFTCommitResult> {

  @Getter
  private PBFTCommitResult pbftCommitResult;

  public PbftSignCapsule(byte[] data) {
    try {
      pbftCommitResult = PBFTCommitResult.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.error("", e);
    }
  }

  public PbftSignCapsule(ByteString data, List<ByteString> signList) {
    PBFTCommitResult.Builder builder = PBFTCommitResult.newBuilder();
    builder.setData(data).addAllSignature(signList.stream().collect(Collectors.toList()));
    pbftCommitResult = builder.build();
  }

  @Override
  public byte[] getData() {
    return pbftCommitResult.toByteArray();
  }

  @Override
  public PBFTCommitResult getInstance() {
    return pbftCommitResult;
  }
}