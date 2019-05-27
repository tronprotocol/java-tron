package org.tron.common.zksnark;

import com.sun.jna.Pointer;
import lombok.Getter;
import lombok.Setter;
import org.tron.core.exception.ZksnarkException;

public class LibrustzcashParam {

  interface ValidParam {

    void valid() throws ZksnarkException;
  }

  public static class InitZksnarkParams implements ValidParam {

    @Setter
    @Getter
    private byte[] spend_path;
    @Setter
    @Getter
    private int spend_path_len;
    @Setter
    @Getter
    private String spend_hash;
    @Setter
    @Getter
    private byte[] output_path;
    @Setter
    @Getter
    private int output_path_len;
    @Setter
    @Getter
    private String output_hash;

    public InitZksnarkParams(byte[] spend_path, int spend_path_len, String spend_hash,
        byte[] output_path, int output_path_len, String output_hash) throws ZksnarkException {
      this.spend_path = spend_path;
      this.spend_path_len = spend_path_len;
      this.spend_hash = spend_hash;
      this.output_path = output_path;
      this.output_path_len = output_path_len;
      this.output_hash = output_hash;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class Zip32XskMasterParams implements ValidParam {

    @Setter
    @Getter
    private byte[] data;
    @Setter
    @Getter
    private int size;
    @Setter
    @Getter
    private byte[] m_bytes;

    public Zip32XskMasterParams(byte[] data, int size, byte[] m_bytes) throws ZksnarkException {
      this.data = data;
      this.size = size;
      this.m_bytes = m_bytes;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      if (!(m_bytes.length == 169)) {
        throw new ZksnarkException("librustzcash_zip32_xsk_master invalid array size");
      }
    }
  }

  public static class Zip32XskDeriveParams extends Zip32XskMasterParams implements ValidParam {

    public Zip32XskDeriveParams(byte[] data, int size, byte[] m_bytes) throws ZksnarkException {
      super(data, size, m_bytes);
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class Zip32XfvkAddressParams implements ValidParam {

    @Setter
    @Getter
    private byte[] xfvk;
    @Setter
    @Getter
    private byte[] j;
    @Setter
    @Getter
    private byte[] j_ret;
    @Setter
    @Getter
    private byte[] addr_ret;

    public Zip32XfvkAddressParams(byte[] xfvk, byte[] j, byte[] j_ret, byte[] addr_ret)
        throws ZksnarkException {
      this.xfvk = xfvk;
      this.j = j;
      this.j_ret = j_ret;
      this.addr_ret = addr_ret;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class CrhIvkParams implements ValidParam {

    @Setter
    @Getter
    private byte[] ak;
    @Setter
    @Getter
    private byte[] nk;
    @Setter
    @Getter
    private byte[] ivk;

    public CrhIvkParams(byte[] ak, byte[] nk, byte[] ivk) throws ZksnarkException {
      this.ak = ak;
      this.nk = nk;
      this.ivk = ivk;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class SaplingKaAgreeParams implements ValidParam {

    @Setter
    @Getter
    private byte[] p;
    @Setter
    @Getter
    private byte[] sk;
    @Setter
    @Getter
    private byte[] result;

    public SaplingKaAgreeParams(byte[] p, byte[] sk, byte[] result) throws ZksnarkException {
      this.p = p;
      this.sk = sk;
      this.result = result;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class SaplingComputeCmParams implements ValidParam {

    @Setter
    @Getter
    private byte[] d;
    @Setter
    @Getter
    private byte[] pk_d;
    @Setter
    @Getter
    private long value;
    @Setter
    @Getter
    private byte[] r;
    @Setter
    @Getter
    private byte[] cm;

    public SaplingComputeCmParams(byte[] d, byte[] pk_d, long value, byte[] r, byte[] cm)
        throws ZksnarkException {
      this.d = d;
      this.pk_d = pk_d;
      this.value = value;
      this.r = r;
      this.cm = cm;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class SaplingComputeNfParams implements ValidParam {

    @Setter
    @Getter
    private byte[] d;
    @Setter
    @Getter
    private byte[] pk_d;
    @Setter
    @Getter
    private long value;
    @Setter
    @Getter
    private byte[] r;
    @Setter
    @Getter
    private byte[] ak;
    @Setter
    @Getter
    private byte[] nk;
    @Setter
    @Getter
    private long position;
    @Setter
    @Getter
    private byte[] result;

    public SaplingComputeNfParams(byte[] d, byte[] pk_d, long value, byte[] r, byte[] ak, byte[] nk,
        long position, byte[] result) throws ZksnarkException {
      this.d = d;
      this.pk_d = pk_d;
      this.value = value;
      this.r = r;
      this.ak = ak;
      this.nk = nk;
      this.position = position;
      this.result = result;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class SaplingKaDerivepublicParams implements ValidParam {

    @Setter
    @Getter
    private byte[] diversifier;
    @Setter
    @Getter
    private byte[] esk;
    @Setter
    @Getter
    private byte[] result;

    public SaplingKaDerivepublicParams(byte[] diversifier, byte[] esk, byte[] result)
        throws ZksnarkException {
      this.diversifier = diversifier;
      this.esk = esk;
      this.result = result;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class SaplingSpendProofParams implements ValidParam {

    @Setter
    @Getter
    private Pointer ctx;
    @Setter
    @Getter
    private byte[] ak;
    @Setter
    @Getter
    private byte[] nsk;
    @Setter
    @Getter
    private byte[] d;
    @Setter
    @Getter
    private byte[] r;
    @Setter
    @Getter
    private byte[] alpha;
    @Setter
    @Getter
    private long value;
    @Setter
    @Getter
    private byte[] anchor;
    @Setter
    @Getter
    private byte[] voucherPath;
    @Setter
    @Getter
    private byte[] cv;
    @Setter
    @Getter
    private byte[] rk;
    @Setter
    @Getter
    private byte[] zkproof;

    public SaplingSpendProofParams(Pointer ctx, byte[] ak, byte[] nsk, byte[] d, byte[] r,
        byte[] alpha, long value, byte[] anchor, byte[] voucherPath, byte[] cv, byte[] rk,
        byte[] zkproof) throws ZksnarkException {
      this.ctx = ctx;
      this.ak = ak;
      this.nsk = nsk;
      this.d = d;
      this.r = r;
      this.alpha = alpha;
      this.value = value;
      this.anchor = anchor;
      this.voucherPath = voucherPath;
      this.cv = cv;
      this.rk = rk;
      this.zkproof = zkproof;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class SaplingOutputProofParams implements ValidParam {

    @Setter
    @Getter
    private Pointer ctx;
    @Setter
    @Getter
    private byte[] esk;
    @Setter
    @Getter
    private byte[] d;
    @Setter
    @Getter
    private byte[] pk_d;
    @Setter
    @Getter
    private byte[] r;
    @Setter
    @Getter
    private long value;
    @Setter
    @Getter
    private byte[] cv;
    @Setter
    @Getter
    private byte[] zkproof;

    public SaplingOutputProofParams(Pointer ctx, byte[] esk, byte[] d, byte[] pk_d, byte[] r,
        long value, byte[] cv, byte[] zkproof) throws ZksnarkException {
      this.ctx = ctx;
      this.esk = esk;
      this.d = d;
      this.pk_d = pk_d;
      this.r = r;
      this.value = value;
      this.cv = cv;
      this.zkproof = zkproof;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class SaplingSpendSigParams implements ValidParam {

    @Setter
    @Getter
    private byte[] ask;
    @Setter
    @Getter
    private byte[] alpha;
    @Setter
    @Getter
    private byte[] sigHash;
    @Setter
    @Getter
    private byte[] result;

    public SaplingSpendSigParams(byte[] ask, byte[] alpha, byte[] sigHash, byte[] result)
        throws ZksnarkException {
      this.ask = ask;
      this.alpha = alpha;
      this.sigHash = sigHash;
      this.result = result;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class SaplingBindingSigParams implements ValidParam {

    @Setter
    @Getter
    private Pointer ctx;
    @Setter
    @Getter
    private long valueBalance;
    @Setter
    @Getter
    private byte[] sighash;
    @Setter
    @Getter
    private byte[] result;

    public SaplingBindingSigParams(Pointer ctx, long valueBalance, byte[] sighash, byte[] result)
        throws ZksnarkException {
      this.ctx = ctx;
      this.valueBalance = valueBalance;
      this.sighash = sighash;
      this.result = result;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class SaplingCheckSpendParams implements ValidParam {

    @Setter
    @Getter
    private Pointer ctx;
    @Setter
    @Getter
    private byte[] cv;
    @Setter
    @Getter
    private byte[] anchor;
    @Setter
    @Getter
    private byte[] nullifier;
    @Setter
    @Getter
    private byte[] rk;
    @Setter
    @Getter
    private byte[] zkproof;
    @Setter
    @Getter
    private byte[] spendAuthSig;
    @Setter
    @Getter
    private byte[] sighashValue;

    public SaplingCheckSpendParams(Pointer ctx, byte[] cv, byte[] anchor, byte[] nullifier,
        byte[] rk, byte[] zkproof, byte[] spendAuthSig, byte[] sighashValue)
        throws ZksnarkException {
      this.ctx = ctx;
      this.cv = cv;
      this.anchor = anchor;
      this.nullifier = nullifier;
      this.rk = rk;
      this.zkproof = zkproof;
      this.spendAuthSig = spendAuthSig;
      this.sighashValue = sighashValue;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class SaplingCheckOutputParams implements ValidParam {

    @Setter
    @Getter
    private Pointer ctx;
    @Setter
    @Getter
    private byte[] cv;
    @Setter
    @Getter
    private byte[] cm;
    @Setter
    @Getter
    private byte[] ephemeralKey;
    @Setter
    @Getter
    private byte[] zkproof;

    public SaplingCheckOutputParams(Pointer ctx, byte[] cv, byte[] cm, byte[] ephemeralKey,
        byte[] zkproof) throws ZksnarkException {
      this.ctx = ctx;
      this.cv = cv;
      this.cm = cm;
      this.ephemeralKey = ephemeralKey;
      this.zkproof = zkproof;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class SaplingFinalCheckParams implements ValidParam {

    @Setter
    @Getter
    private Pointer ctx;
    @Setter
    @Getter
    private long valueBalance;
    @Setter
    @Getter
    private byte[] bindingSig;
    @Setter
    @Getter
    private byte[] sighashValue;

    public SaplingFinalCheckParams(Pointer ctx, long valueBalance, byte[] bindingSig,
        byte[] sighashValue) throws ZksnarkException {
      this.ctx = ctx;
      this.valueBalance = valueBalance;
      this.bindingSig = bindingSig;
      this.sighashValue = sighashValue;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class IvkToPkdParams implements ValidParam {

    @Setter
    @Getter
    private byte[] ivk;
    @Setter
    @Getter
    private byte[] d;
    @Setter
    @Getter
    private byte[] pk_d;

    public IvkToPkdParams(byte[] ivk, byte[] d, byte[] pk_d) throws ZksnarkException {
      this.ivk = ivk;
      this.d = d;
      this.pk_d = pk_d;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }

  public static class MerkleHashParams implements ValidParam {

    @Setter
    @Getter
    private int depth;
    @Setter
    @Getter
    private byte[] a;
    @Setter
    @Getter
    private byte[] b;
    @Setter
    @Getter
    private byte[] result;

    public MerkleHashParams(int depth, byte[] a, byte[] b, byte[] result) throws ZksnarkException {
      this.depth = depth;
      this.a = a;
      this.b = b;
      this.result = result;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {

    }
  }
}
