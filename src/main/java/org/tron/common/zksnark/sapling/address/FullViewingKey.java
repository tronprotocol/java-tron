package org.tron.common.zksnark.sapling.address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.zksnark.sapling.Librustzcash;

// Decryption using a Full Viewing Key

@AllArgsConstructor
public class FullViewingKey {

  @Getter
  @Setter
  private byte[] ak; // 256
  @Getter
  @Setter
  private byte[] nk; // 256
  @Getter
  @Setter
  private byte[] ovk; // 256,the outgoing viewing key

  // ! Get the fingerprint of this full viewing key (as defined in ZIP 32).
  byte[] GetFingerprint() {
    return CBLAKE2bWriter.GetHash(this);
  }

  public IncomingViewingKey in_viewing_key() {

    byte[] ivk = null; // the incoming viewing key
    Librustzcash.librustzcashCrhIvk(ak, nk, ivk);
    return new IncomingViewingKey(ivk);
  }

  boolean is_valid() {
    byte[] ivk = null;
    Librustzcash.librustzcashCrhIvk(ak, nk, ivk);
    return ivk != null && ivk.length != 0;
  }

  public static class CBLAKE2bWriter {

    public static byte[] GetHash(FullViewingKey keys) {
      return null;
    }
  }
}
