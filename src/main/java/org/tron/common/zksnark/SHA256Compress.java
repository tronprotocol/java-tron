package org.tron.common.zksnark;


import org.tron.common.utils.Sha256Hash;

public class SHA256Compress {

  private byte[] content;

  public SHA256Compress() {
    content = new byte[256];
  }

  public SHA256Compress(byte[] content) {
    this.content = content;
  }

  public byte[] getContent() {
    return content;
  }

  public static SHA256Compress combine(
      final SHA256Compress a,
      final SHA256Compress b,
      int depth
  ) {
    byte[] res;
    byte[] blob = new byte[64];

    System.arraycopy(a.getContent(), 0, blob, 0, 32);
    System.arraycopy(b.getContent(), 0, blob, 32, 32);
    res = Sha256Hash.hash(blob);

    return new SHA256Compress(res);
  }

  static public SHA256Compress uncommitted() {
    return new SHA256Compress();
  }

}
