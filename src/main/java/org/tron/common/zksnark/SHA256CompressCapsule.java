package org.tron.common.zksnark;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.crypto.zksnark.ZksnarkUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Update;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.protos.Contract.SHA256Compress;

@Slf4j
public class SHA256CompressCapsule implements ProtoCapsule<SHA256Compress> {

  private SHA256Compress sha256Compress;

  public SHA256CompressCapsule() {
    this.sha256Compress = SHA256Compress.getDefaultInstance();
  }

  public SHA256CompressCapsule(final SHA256Compress sha256Compress) {
    this.sha256Compress = sha256Compress;
  }

  public SHA256CompressCapsule(final byte[] data) {
    try {
      this.sha256Compress = SHA256Compress.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public ByteString getContent() {
    return this.sha256Compress.getContent();
  }

  public void setContent(ByteString content) {
    this.sha256Compress = SHA256Compress.newBuilder().setContent(content).build();
  }

  @Override
  public byte[] getData() {
    return this.sha256Compress.toByteArray();
  }

  @Override
  public SHA256Compress getInstance() {
    return this.sha256Compress;
  }

  public static SHA256CompressCapsule combine(
      final SHA256Compress a, final SHA256Compress b, int depth) {
    byte[] res;
    byte[] blob = new byte[64];

    System.arraycopy(a.getContent().toByteArray(), 0, blob, 0, 32);
    System.arraycopy(b.getContent().toByteArray(), 0, blob, 32, 32);
    res = Sha256Update.Sha256OneBlock(blob);
    SHA256CompressCapsule sha256CompressCapsule = new SHA256CompressCapsule();
    sha256CompressCapsule.setContent(ByteString.copyFrom(res));

    byte[] temp = a.getContent().toByteArray();
    ZksnarkUtils.sort(temp);
    System.out.print(ByteArray.toHexString(temp));
    System.out.print(" : ");
    temp = b.getContent().toByteArray();
    ZksnarkUtils.sort(temp);
    System.out.print(ByteArray.toHexString(temp));
    System.out.print(" : ");
    ZksnarkUtils.sort(res);
    System.out.println(ByteArray.toHexString(res));
    return sha256CompressCapsule;
  }

  public static SHA256CompressCapsule uncommitted() {
    SHA256CompressCapsule compressCapsule = new SHA256CompressCapsule();
    compressCapsule.setContent(
        ByteString.copyFrom(
            ByteArray.fromHexString(
                "0000000000000000000000000000000000000000000000000000000000000000")));
    return compressCapsule;
  }

  public boolean isPresent() {
    return !sha256Compress.getContent().isEmpty();
  }

  public static void main(String[] args) {
    byte[] a =
        ByteArray.fromHexString("c9b350fc5221e4b4db929307a628ec6ca751f309d0653608d90e41ad5a2dc1d4");
    ZksnarkUtils.sort(a);
    byte[] b =
        ByteArray.fromHexString("d8a93718eaf9feba4362d2c091d4e58ccabe9f779957336269b4b917be9856da");
    ZksnarkUtils.sort(b);
    SHA256Compress sa = SHA256Compress.newBuilder().setContent(ByteString.copyFrom(a)).build();
    SHA256Compress sb = SHA256Compress.newBuilder().setContent(ByteString.copyFrom(b)).build();
    combine(sa, sb, 0);
  }
}
