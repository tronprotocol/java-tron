package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.MerkleHashParams;
import org.tron.core.exception.ZksnarkException;
import org.tron.protos.contract.ShieldContract.PedersenHash;


@Slf4j
public class PedersenHashCapsule implements ProtoCapsule<PedersenHash> {

  private PedersenHash pedersenHash;

  public PedersenHashCapsule() {
    this.pedersenHash = PedersenHash.getDefaultInstance();
  }

  public PedersenHashCapsule(final PedersenHash pedersenHash) {
    this.pedersenHash = pedersenHash;
  }

  public PedersenHashCapsule(final byte[] data) {
    try {
      this.pedersenHash = PedersenHash.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public static PedersenHashCapsule combine(final PedersenHash a, final PedersenHash b, int depth)
      throws ZksnarkException {
    byte[] res = new byte[32];

    JLibrustzcash.librustzcashMerkleHash(new MerkleHashParams(depth, a.getContent().toByteArray(),
        b.getContent().toByteArray(), res));

    PedersenHashCapsule pedersenHashCapsule = new PedersenHashCapsule();
    pedersenHashCapsule.setContent(ByteString.copyFrom(res));

    return pedersenHashCapsule;
  }

  public static PedersenHashCapsule uncommitted() throws ZksnarkException {
    byte[] res = new byte[32];

    JLibrustzcash.librustzcashTreeUncommitted(res);

    PedersenHashCapsule compressCapsule = new PedersenHashCapsule();
    compressCapsule.setContent(ByteString.copyFrom(res));

    return compressCapsule;
  }

  public static void main(String[] args) {
    try {
      byte[] a =
          ByteArray
              .fromHexString("05655316a07e6ec8c9769af54ef98b30667bfb6302b32987d552227dae86a087");
      byte[] b =
          ByteArray
              .fromHexString("06041357de59ba64959d1b60f93de24dfe5ea1e26ed9e8a73d35b225a1845ba7");

      PedersenHash sa = PedersenHash.newBuilder().setContent(ByteString.copyFrom(a)).build();
      PedersenHash sb = PedersenHash.newBuilder().setContent(ByteString.copyFrom(b)).build();

      PedersenHash result = combine(sa, sb, 25).getInstance();
      // 61a50a5540b4944da27cbd9b3d6ec39234ba229d2c461f4d719bc136573bf45b
      System.out.println(ByteArray.toHexString(result.getContent().toByteArray()));
    } catch (ZksnarkException e) {
      e.printStackTrace();
    }
  }

  public ByteString getContent() {
    return this.pedersenHash.getContent();
  }

  public void setContent(ByteString content) {
    this.pedersenHash = PedersenHash.newBuilder().setContent(content).build();
  }

  @Override
  public byte[] getData() {
    return this.pedersenHash.toByteArray();
  }

  @Override
  public PedersenHash getInstance() {
    return this.pedersenHash;
  }

  public boolean isPresent() {
    return !pedersenHash.getContent().isEmpty();
  }
}
