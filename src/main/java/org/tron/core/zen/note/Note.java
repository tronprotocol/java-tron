package org.tron.core.zen.note;

import org.tron.common.zksnark.Librustzcash;
import org.tron.common.zksnark.LibrustzcashParam.ComputeCmParams;
import org.tron.common.zksnark.LibrustzcashParam.ComputeNfParams;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.PaymentAddress;

public class Note {

  public long value = 0;
  public DiversifierT d;
  public byte[] pkD; // 256
  public byte[] r; // 256

  public Note(PaymentAddress address, long value) throws ZksnarkException {
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

  public static byte[] generateR() throws ZksnarkException {
    byte[] r = new byte[32];
    Librustzcash.librustzcashSaplingGenerateR(r);
    return r;
  }

  // Call librustzcash to compute the commitment
  public byte[] cm() throws ZksnarkException {
    byte[] result = new byte[32];
    if (!Librustzcash.librustzcashComputeCm(
        new ComputeCmParams(d.getData(), pkD, value, r, result))) {
      return null;
    }
    return result;
  }

  public byte[] nullifier(FullViewingKey vk, long position) throws ZksnarkException {
    byte[] ak = vk.getAk();
    byte[] nk = vk.getNk();
    byte[] result = new byte[32]; // 256
    if (!Librustzcash.librustzcashComputeNf(
        new ComputeNfParams(d.getData(), pkD, value, r, ak, nk, position, result))) {
      return null;
    }
    return result;
  }

  public byte[] nullifier(byte[] ak, byte[] nk, long position) throws ZksnarkException {
    byte[] result = new byte[32]; // 256
    if (!Librustzcash.librustzcashComputeNf(
        new ComputeNfParams(d.getData(), pkD, value, r, ak, nk, position, result))) {
      return null;
    }
    return result;
  }
}
