package org.tron.consensus.pbft.message;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Protocol.PbftMessage;
import org.tron.protos.Protocol.PbftMessage.Raw;
import org.tron.protos.Protocol.PbftMessage.Type;

public class PbftSrMessage extends PbftBaseMessage {

  public PbftSrMessage() {

  }

  public PbftSrMessage(byte[] data) throws Exception {
    super(MessageTypes.PBFT_SR_MSG.asByte(), data);
  }

  @Override
  public String getNo() {
    return pbftMessage.getRawData().getBlockNum() + "_" + getType().asByte();
  }

  @Override
  public PbftBaseMessage createMessage() {
    return new PbftSrMessage();
  }

  public static PbftBaseMessage buildPrePrepareMessage(BlockCapsule block,
      List<ByteString> currentWitness) {
    List<String> srStringList = currentWitness.stream()
        .map(sr -> ByteArray.toHexString(sr.toByteArray())).collect(Collectors.toList());
    PbftSrMessage pbftSrMessage = new PbftSrMessage();
    Miner miner = Param.getInstance().getMiner();
    ECKey ecKey = ECKey.fromPrivate(miner.getPrivateKey());
    Raw.Builder rawBuilder = Raw.newBuilder();
    PbftMessage.Builder builder = PbftMessage.newBuilder();
    byte[] publicKey = ecKey.getAddress();
    rawBuilder.setBlockNum(block.getNum())
        .setPbftMsgType(Type.PREPREPARE)
        .setTime(System.currentTimeMillis())
        .setPublicKey(ByteString.copyFrom(publicKey == null ? new byte[0] : publicKey))
        .setData(ByteString.copyFromUtf8(JSON.toJSONString(srStringList)));
    Raw raw = rawBuilder.build();
    byte[] hash = Sha256Hash.hash(raw.toByteArray());
    ECDSASignature signature = ecKey.sign(hash);
    builder.setRawData(raw).setSign(ByteString.copyFrom(signature.toByteArray()));
    PbftMessage message = builder.build();
    pbftSrMessage.setType(MessageTypes.PBFT_SR_MSG.asByte())
        .setPbftMessage(message).setData(message.toByteArray()).setSwitch(block.isSwitch());
    return pbftSrMessage;
  }
}
