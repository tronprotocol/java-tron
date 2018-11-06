package org.tron.common.zksnark;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
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
    this.sha256Compress = SHA256Compress.newBuilder()
        .setContent(content)
        .build();
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
      final SHA256Compress a,
      final SHA256Compress b,
      int depth
  ) {
    byte[] res;
    byte[] blob = new byte[64];

    System.arraycopy(a.getContent().toByteArray(), 0, blob, 0, 32);
    System.arraycopy(b.getContent().toByteArray(), 0, blob, 32, 32);
    res = Sha256Hash.hash(blob);
    SHA256CompressCapsule sha256CompressCapsule = new SHA256CompressCapsule();
    sha256CompressCapsule.setContent(ByteString.copyFrom(res));

    return sha256CompressCapsule;
  }

  static public SHA256CompressCapsule uncommitted() {
    SHA256CompressCapsule compressCapsule = new SHA256CompressCapsule();
    compressCapsule.setContent(ByteString.copyFrom(ByteArray
        .fromHexString("0000000000000000000000000000000000000000000000000000000000000000")));
    return compressCapsule;
  }


  public boolean isExist() {
    return !sha256Compress.getContent().isEmpty();
  }

}
