package org.tron.consensus.pbft.message;

import com.google.protobuf.ByteString;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Protocol.PbftMessage;
import org.tron.protos.Protocol.PbftMessage.Raw;
import org.tron.protos.Protocol.PbftMessage.Type;

public class PbftBlockMessage extends PbftBaseMessage {

  public PbftBlockMessage() {
  }

  public PbftBlockMessage(byte[] data) throws Exception {
    super(MessageTypes.PBFT_BLOCK_MSG.asByte(), data);
  }

  public String getNo() {
    return pbftMessage.getRawData().getBlockNum() + "_" + getType().asByte();
  }

  @Override
  public PbftBaseMessage createMessage() {
    return new PbftBlockMessage();
  }

  public static PbftBaseMessage buildPrePrepareMessage(BlockCapsule blockCapsule) {
    PbftBlockMessage pbftBlockMessage = new PbftBlockMessage();
    Miner miner = Param.getInstance().getMiner();
    ECKey ecKey = ECKey.fromPrivate(miner.getPrivateKey());
    Raw.Builder rawBuilder = Raw.newBuilder();
    PbftMessage.Builder builder = PbftMessage.newBuilder();
    byte[] publicKey = ecKey.getAddress();
    rawBuilder.setBlockNum(blockCapsule.getNum())
        .setPbftMsgType(Type.PREPREPARE)
        .setTime(System.currentTimeMillis())
        .setPublicKey(ByteString.copyFrom(publicKey == null ? new byte[0] : publicKey))
        .setData(blockCapsule.getBlockId().getByteString());
    Raw raw = rawBuilder.build();
    byte[] hash = Sha256Hash.hash(raw.toByteArray());
    ECDSASignature signature = ecKey.sign(hash);
    builder.setRawData(raw).setSign(ByteString.copyFrom(signature.toByteArray()));
    PbftMessage message = builder.build();
    pbftBlockMessage.setType(MessageTypes.PBFT_BLOCK_MSG.asByte())
        .setPbftMessage(message).setData(message.toByteArray()).setSwitch(blockCapsule.isSwitch());
    return pbftBlockMessage;
  }

}
