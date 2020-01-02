package org.tron.consensus.pbft.message;

import com.google.protobuf.ByteString;
import java.util.List;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Protocol.PBFTMessage;
import org.tron.protos.Protocol.PBFTMessage.Raw;
import org.tron.protos.Protocol.PBFTMessage.Type;
import org.tron.protos.Protocol.SRL;

public class PbftSrMessage extends PbftBaseMessage {

  public PbftSrMessage() {

  }

  public PbftSrMessage(byte[] data) throws Exception {
    super(MessageTypes.PBFT_SR_MSG.asByte(), data);
  }

  @Override
  public String getNo() {
    return pbftMessage.getRawData().getNumber() + "_" + getType().asByte();
  }

  @Override
  public PbftBaseMessage createMessage() {
    return new PbftSrMessage();
  }

  public static PbftBaseMessage buildPrePrepareMessage(BlockCapsule block,
      List<ByteString> currentWitness, long epoch) {
    SRL.Builder srListBuilder = SRL.newBuilder();
    byte[] data = srListBuilder.addAllSrAddress(currentWitness).setEpoch(epoch).build()
        .toByteArray();
    PbftSrMessage pbftSrMessage = new PbftSrMessage();
    Miner miner = Param.getInstance().getMiner();
    ECKey ecKey = ECKey.fromPrivate(miner.getPrivateKey());
    Raw.Builder rawBuilder = Raw.newBuilder();
    PBFTMessage.Builder builder = PBFTMessage.newBuilder();
    rawBuilder.setNumber(block.getNum())
        .setPbftMsgType(Type.PREPREPARE)
        .setData(ByteString.copyFrom(data));
    Raw raw = rawBuilder.build();
    byte[] hash = Sha256Hash.hash(raw.toByteArray());
    ECDSASignature signature = ecKey.sign(hash);
    builder.setRawData(raw).setSignature(ByteString.copyFrom(signature.toByteArray()));
    PBFTMessage message = builder.build();
    pbftSrMessage.setType(MessageTypes.PBFT_SR_MSG.asByte())
        .setPbftMessage(message).setData(message.toByteArray()).setSwitch(block.isSwitch());
    return pbftSrMessage;
  }

  public static PbftBaseMessage buildFullNodePrePrepareMessage(BlockCapsule block,
      List<ByteString> currentWitness, long epoch) {
    SRL.Builder srListBuilder = SRL.newBuilder();
    byte[] data = srListBuilder.addAllSrAddress(currentWitness).setEpoch(epoch).build()
        .toByteArray();
    PbftSrMessage pbftSrMessage = new PbftSrMessage();
    Raw.Builder rawBuilder = Raw.newBuilder();
    PBFTMessage.Builder builder = PBFTMessage.newBuilder();
    rawBuilder.setNumber(block.getNum())
        .setPbftMsgType(Type.PREPREPARE)
        .setData(ByteString.copyFrom(data));
    Raw raw = rawBuilder.build();
    builder.setRawData(raw);
    PBFTMessage message = builder.build();
    pbftSrMessage.setType(MessageTypes.PBFT_SR_MSG.asByte())
        .setPbftMessage(message).setData(message.toByteArray()).setSwitch(block.isSwitch());
    return pbftSrMessage;
  }
}
