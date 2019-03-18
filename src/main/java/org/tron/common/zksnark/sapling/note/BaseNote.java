package org.tron.common.zksnark.sapling.note;

import lombok.AllArgsConstructor;
import org.tron.common.zksnark.sapling.Librustzcash;
import org.tron.common.zksnark.sapling.address.FullViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.address.DiversifierT;

public class BaseNote {

  public long value = 0;

  @AllArgsConstructor
  public static class Note extends BaseNote {

    public DiversifierT d;
    public byte[] pk_d; // 256
    public byte[] r; // 256

    // Construct and populate Sapling note for a given payment address and value.
    public Note(PaymentAddress address, long value) {
      this.value = value;
      d = address.getD();
      pk_d = address.getPkD();
      Librustzcash.librustzcashSaplingGenerateR(r);
    }

    // Call librustzcash to compute the commitment
    public byte[] cm() {
      byte[] result = null;
      if (!Librustzcash.librustzcashSaplingComputeCm(d.getData(), pk_d, value, r, result)) {
        return null;
      }

      return result;
    }

    // Call librustzcash to compute the nullifier

    // position 64
    public byte[] nullifier(FullViewingKey vk, long position) {
      byte[] ak = vk.getAk();
      byte[] nk = vk.getNk();

      byte[] result = null; // 256
      if (!Librustzcash.librustzcashSaplingComputeNf(
          d.getData(), pk_d, value, r, ak, nk, position, result)) {
        return null;
      }

      return result;
    }
  }
}
