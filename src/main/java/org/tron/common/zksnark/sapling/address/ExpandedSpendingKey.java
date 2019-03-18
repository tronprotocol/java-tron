package org.tron.common.zksnark.sapling.address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.sapling.Librustzcash;

@AllArgsConstructor
public class ExpandedSpendingKey {

  @Setter
  @Getter
  byte[] ask; // the spend authorizing key,256
  @Setter
  @Getter
  byte[] nsk; // the proof authorizing key (ak, nsk),256
  // Let ovk be an outgoing viewing key that is intended to be able to decrypt this payment
  @Setter
  @Getter
  byte[] ovk; // the outgoing viewing key,256

  // A note is spent by proving knowledge of (p, ak, nsk) in zero knowledge while
  // publically disclosing its nf, allowing nf to be used to prevent double-spending.

  public ExpandedSpendingKey() {
  }

  public FullViewingKey full_viewing_key() {

    byte[] ak = Librustzcash.librustzcashAskToAk(ask); // 256
    byte[] nk = Librustzcash.librustzcashNskToNk(nsk); // 256

    return new FullViewingKey(ak, nk, ovk);
  }

  public static ExpandedSpendingKey decode(byte[] m_bytes) {
    ExpandedSpendingKey key = new ExpandedSpendingKey();

    byte[] ask = ByteArray.subArray(m_bytes, 0, 32);
    byte[] nsk = ByteArray.subArray(m_bytes, 32, 64);
    byte[] ovk = ByteArray.subArray(m_bytes, 64, 96);
    key.setAsk(ask);
    key.setNsk(nsk);
    key.setOvk(ovk);
    return key;
  }

  public byte[] encode() {

    byte[] m_bytes = new byte[96];

    System.arraycopy(ask, 0, m_bytes, 0, 32);
    System.arraycopy(nsk, 0, m_bytes, 32, 32);
    System.arraycopy(ovk, 0, m_bytes, 64, 32);

    return m_bytes;
  }
}
