package org.tron.common.crypto;


import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Rsv {

  private final byte[] r;
  private final byte[] s;
  private final byte v;


  public static Rsv fromSignature(byte[] sign) {
    if (sign == null || sign.length < 65) {
      throw new IllegalArgumentException(
          "Invalid signature length: " + (sign == null ? "null" : sign.length));
    }
    byte[] r = Arrays.copyOfRange(sign, 0, 32);
    byte[] s = Arrays.copyOfRange(sign, 32, 64);
    byte v = sign[64];
    if (v < 27) {
      v += 27; //revId -> v
    }
    return new Rsv(r, s, v);
  }
}
