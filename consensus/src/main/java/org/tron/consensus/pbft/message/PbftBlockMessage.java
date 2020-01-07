package org.tron.consensus.pbft.message;

import com.google.protobuf.ByteString;
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

public class PbftBlockMessage extends PbftBaseMessage {

  public PbftBlockMessage() {
  }

  public PbftBlockMessage(byte[] data) throws Exception {
    super(MessageTypes.PBFT_BLOCK_MSG.asByte(), data);
  }

  public String getNo() {
    return pbftMessage.getRawData().getNumber() + "_" + getType().asByte();
  }

  @Override
  public PbftBaseMessage createMessage() {
    return new PbftBlockMessage();
  }

  public static PbftBaseMessage buildPrePrepareMessage(BlockCapsule blockCapsule, long epoch) {
    PbftBlockMessage pbftBlockMessage = new PbftBlockMessage();
    Miner miner = Param.getInstance().getMiner();
    ECKey ecKey = ECKey.fromPrivate(miner.getPrivateKey());
    PBFTMessage.Builder builder = PBFTMessage.newBuilder();
    Raw.Builder rawBuilder = Raw.newBuilder();
    rawBuilder.setNumber(blockCapsule.getNum())
        .setPbftMsgType(Type.PREPREPARE).setEpoch(epoch)
        .setData(blockCapsule.getBlockId().getByteString());
    Raw raw = rawBuilder.build();
    byte[] hash = Sha256Hash.hash(raw.toByteArray());
    ECDSASignature signature = ecKey.sign(hash);
    builder.setRawData(raw).setSignature(ByteString.copyFrom(signature.toByteArray()));
    PBFTMessage message = builder.build();
    pbftBlockMessage.setType(MessageTypes.PBFT_BLOCK_MSG.asByte())
        .setPbftMessage(message).setData(message.toByteArray()).setSwitch(blockCapsule.isSwitch());
    return pbftBlockMessage;
  }

  public static PbftBaseMessage buildFullNodePrePrepareMessage(BlockCapsule blockCapsule,
      long epoch) {
    PbftBlockMessage pbftBlockMessage = new PbftBlockMessage();
    PBFTMessage.Builder builder = PBFTMessage.newBuilder();
    Raw.Builder rawBuilder = Raw.newBuilder();
    rawBuilder.setNumber(blockCapsule.getNum())
        .setPbftMsgType(Type.PREPREPARE).setEpoch(epoch)
        .setData(blockCapsule.getBlockId().getByteString());
    Raw raw = rawBuilder.build();
    builder.setRawData(raw);
    PBFTMessage message = builder.build();
    pbftBlockMessage.setType(MessageTypes.PBFT_BLOCK_MSG.asByte())
        .setPbftMessage(message).setData(message.toByteArray()).setSwitch(blockCapsule.isSwitch());
    return pbftBlockMessage;
  }

}
