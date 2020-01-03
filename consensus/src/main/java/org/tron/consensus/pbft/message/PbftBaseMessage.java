package org.tron.consensus.pbft.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.security.SignatureException;
import java.util.stream.Collectors;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.WalletUtil;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Protocol.PBFTMessage;
import org.tron.protos.Protocol.PBFTMessage.Raw;
import org.tron.protos.Protocol.PBFTMessage.Type;
import org.tron.protos.Protocol.SRL;

public abstract class PbftBaseMessage extends Message {

  protected PBFTMessage pbftMessage;

  private boolean isSwitch;

  private byte[] publicKey;

  public PbftBaseMessage() {
  }

  public PbftBaseMessage(byte type, byte[] data) throws IOException, P2pException {
    super(type, data);
    this.pbftMessage = PBFTMessage.parseFrom(getCodedInputStream(data));
    if (isFilter()) {
      compareBytes(data, pbftMessage.toByteArray());
    }
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public PBFTMessage getPbftMessage() {
    return pbftMessage;
  }

  public PbftBaseMessage setPbftMessage(PBFTMessage pbftMessage) {
    this.pbftMessage = pbftMessage;
    return this;
  }

  public boolean isSwitch() {
    return isSwitch;
  }

  public PbftBaseMessage setSwitch(boolean aSwitch) {
    isSwitch = aSwitch;
    return this;
  }

  public PbftBaseMessage setData(byte[] data) {
    this.data = data;
    return this;
  }

  public PbftBaseMessage setType(byte type) {
    this.type = type;
    return this;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }

  public String getKey() {
    return getNo() + "_" + Hex.toHexString(publicKey);
  }

  public String getDataKey() {
    return getNo() + "_" + Hex.toHexString(pbftMessage.getRawData().getData().toByteArray());
  }

  public long getNumber() {
    return pbftMessage.getRawData().getNumber();
  }

  public abstract String getNo();

  public abstract PbftBaseMessage createMessage();

  public void analyzeSignature()
      throws SignatureException {
    byte[] hash = Sha256Hash.hash(getPbftMessage().getRawData().toByteArray());
    publicKey = ECKey.signatureToAddress(hash, TransactionCapsule
        .getBase64FromByteString(getPbftMessage().getSignature()));
  }

  public PbftBaseMessage buildPrePareMessage() {
    return buildMessageCapsule(Type.PREPARE);
  }

  public PbftBaseMessage buildCommitMessage() {
    return buildMessageCapsule(Type.COMMIT);
  }

  private PbftBaseMessage buildMessageCapsule(Type type) {
    PbftBaseMessage pbftMessage = createMessage();
    Miner miner = Param.getInstance().getMiners().get(0);
    ECKey ecKey = ECKey.fromPrivate(miner.getPrivateKey());
    PBFTMessage.Builder builder = PBFTMessage.newBuilder();
    Raw.Builder rawBuilder = Raw.newBuilder();
    rawBuilder.setNumber(getPbftMessage().getRawData().getNumber())
        .setPbftMsgType(type)
        .setData(getPbftMessage().getRawData().getData());
    Raw raw = rawBuilder.build();
    byte[] hash = Sha256Hash.hash(raw.toByteArray());
    ECDSASignature signature = ecKey.sign(hash);
    builder.setRawData(raw).setSignature(ByteString.copyFrom(signature.toByteArray()));
    PBFTMessage message = builder.build();
    pbftMessage.setType(getType().asByte())
        .setPbftMessage(message).setData(message.toByteArray());
    return pbftMessage;
  }

  @Override
  public String toString() {
    return "PbftMsgType:" + pbftMessage.getRawData().getPbftMsgType()
        + ", node address:" + (ByteUtil.isNullOrZeroArray(publicKey) ? null
        : Hex.toHexString(publicKey))
        + ", number:" + pbftMessage.getRawData().getNumber()
        + ", data:" + getDataString()
        + ", " + super.toString();
  }

  public String getDataString() {
    return getType() == MessageTypes.PBFT_SR_MSG ? decode()
        : Hex.toHexString(pbftMessage.getRawData().getData().toByteArray());
  }

  private String decode() {
    try {
      SRL srList = SRL.parseFrom(pbftMessage.getRawData().getData().toByteArray());
      return "cycle = " + srList.getEpoch() + ", sr list = " + srList.getSrAddressList().stream()
          .map(
              bytes -> WalletUtil.encode58Check(bytes.toByteArray())).collect(Collectors.toList());
    } catch (InvalidProtocolBufferException e) {
    }
    return "decode error";
  }
}
