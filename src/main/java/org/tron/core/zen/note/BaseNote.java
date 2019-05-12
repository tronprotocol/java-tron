package org.tron.core.zen.note;

import com.google.protobuf.ByteString;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.Librustzcash;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.PaymentAddress;

public class BaseNote {

  public long value = 0;

  public static class Note extends BaseNote {

    public DiversifierT d;
    public byte[] pkD; // 256
    public byte[] r; // 256

    public static byte[] generateR() {
      byte[] r = new byte[32];
      Librustzcash.librustzcashSaplingGenerateR(r);
      return r;
    }

    public Note(PaymentAddress address, long value) {
      this.value = value;
      this.d = address.getD();
      this.pkD = address.getPkD();
      r = new byte[32];
      Librustzcash.librustzcashSaplingGenerateR(r);
    }

    public Note(DiversifierT d, byte[] pkD, long value, byte[] r) {
      this.d = d;
      this.pkD = pkD;
      this.value = value;
      this.r = r;
    }

    // Call librustzcash to compute the commitment
    public byte[] cm() {
      byte[] result = new byte[32];
      if (!Librustzcash.librustzcashSaplingComputeCm(d.getData(), pkD, value, r, result)) {
        return null;
      }

      return result;
    }

    // Call librustzcash to compute the nullifier

    // position 64
    public byte[] nullifier(FullViewingKey vk, long position) {
      byte[] ak = vk.getAk();
      byte[] nk = vk.getNk();

      byte[] result = new byte[256]; // 256
      if (!Librustzcash.librustzcashSaplingComputeNf(
          d.getData(), pkD, value, r, ak, nk, position, result)) {
        return null;
      }

      return result;
    }
  }

  public static void main(String[] args) throws Exception {
    byte[] result = new byte[32];
    if (!Librustzcash.librustzcashSaplingComputeCm(
        (ByteArray.fromHexString("fc6eb90855700861de6639")), (
            ByteArray
                .fromHexString("1abfbf64bc4934aaf7f29b9fea995e5a16e654e63dbe07db0ef035499d216e19")),
        9990000000L, (ByteArray.fromHexString("08e3a2ff1101b628147125b786c757b483f1cf7c309f8a647055bfb1ca819c02")), result)) {
      System.out.println(" error");
    } else {
      System.out.println(" ok");
    }

  }
}
