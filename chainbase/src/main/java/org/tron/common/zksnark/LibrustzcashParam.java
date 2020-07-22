package org.tron.common.zksnark;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.exception.ZksnarkException;

public class LibrustzcashParam {

  public static void validNull(byte[] value) throws ZksnarkException {
    if (ByteUtil.isNullOrZeroArray(value)) {
      throw new ZksnarkException("param is null");
    }
  }

  public static void validObjectNull(Object object) throws ZksnarkException {
    if (object == null) {
      throw new ZksnarkException("param is null");
    }
  }

  public static void validByteValue(byte src, byte desc) throws ZksnarkException {
    if (src != desc) {
      throw new ZksnarkException("param " + src + " not equals:" + desc);
    }
  }

  public static void validParamLength(byte[] value, int length) throws ZksnarkException {
    validNull(value);
    if (value.length != length) {
      throw new ZksnarkException("param length must be " + length);
    }
  }

  public static void valid11Params(byte[] value) throws ZksnarkException {
    validParamLength(value, 11);
  }

  public static void valid32Params(byte[] value) throws ZksnarkException {
    validParamLength(value, 32);
  }

  public static void validVoucherPath(byte[] voucherPath) throws ZksnarkException {
    validParamLength(voucherPath, 1 + 33 * 32 + 8);
    validByteValue(voucherPath[0], (byte) 0x20);
    for (int i = 0; i < 32; i++) {
      validByteValue(voucherPath[1 + i * 33], (byte) 0x20);
    }
  }

  public static void validValueParams(long value) throws ZksnarkException {
    if (value < 0) {
      throw new ZksnarkException("Value should be non-negative.");
    }
  }

  public static void validPositionParams(long value) throws ZksnarkException {
    if (value < 0) {
      throw new ZksnarkException("Position should be non-negative.");
    }
  }

  interface ValidParam {

    void valid() throws ZksnarkException;
  }

  public static class InitZksnarkParams implements ValidParam {

    @Setter
    @Getter
    private String spend_path;
    @Setter
    @Getter
    private String spend_hash;
    @Setter
    @Getter
    private String output_path;
    @Setter
    @Getter
    private String output_hash;

    public InitZksnarkParams(String spend_path, String spend_hash, String output_path,
        String output_hash) throws ZksnarkException {
      this.spend_path = spend_path;
      this.spend_hash = spend_hash;
      this.output_path = output_path;
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
      validParamLength(m_bytes, 169);
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

  /**
   * (ak,nk)--> ivk ak: spendAuthSig.publickey 32 bytes nk: 32 bytes ivk: incoming viewing key, 32
   * bytes
   */
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
      valid32Params(ak);
      valid32Params(nk);
      valid32Params(ivk);
    }
  }

  /**
   * KaAgree(sk,p)=[h_J*sk]p p: point, 32 bytes sk: 32 bytes result: 32 bytes
   */
  public static class KaAgreeParams implements ValidParam {

    @Setter
    @Getter
    private byte[] p;
    @Setter
    @Getter
    private byte[] sk;
    @Setter
    @Getter
    private byte[] result;

    public KaAgreeParams(byte[] p, byte[] sk, byte[] result) throws ZksnarkException {
      this.p = p;
      this.sk = sk;
      this.result = result;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      valid32Params(p);
      valid32Params(sk);
      valid32Params(result);
    }
  }

  /**
   * Compute note commitment d: diversifier, 11 bytes pkD: 32 bytes r: rcm,  32 bytes cm: note
   * commitment, 32 bytes, value >= 0
   */
  public static class ComputeCmParams implements ValidParam {

    @Setter
    @Getter
    private byte[] d;
    @Setter
    @Getter
    private byte[] pkD;
    @Setter
    @Getter
    private long value;
    @Setter
    @Getter
    private byte[] r;
    @Setter
    @Getter
    private byte[] cm;

    public ComputeCmParams(byte[] d, byte[] pkD, long value, byte[] r, byte[] cm)
        throws ZksnarkException {
      this.d = d;
      this.pkD = pkD;
      this.value = value;
      this.r = r;
      this.cm = cm;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validValueParams(value);
      valid11Params(d);
      valid32Params(pkD);
      valid32Params(r);
      valid32Params(cm);
    }
  }

  /**
   * compute nullifier d: diversifier, 11 bytes pkD, 32 bytes r: rcm,  32 bytes ak:
   * spendAuthSig.PulicKey, 32 bytes nk: to genarate nullifier, 32 bytes result: nullifier, 32
   * bytes, value >= 0, position >= 0
   */
  public static class ComputeNfParams implements ValidParam {

    @Setter
    @Getter
    private byte[] d;
    @Setter
    @Getter
    private byte[] pkD;
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

    public ComputeNfParams(byte[] d, byte[] pkD, long value, byte[] r, byte[] ak, byte[] nk,
        long position, byte[] result) throws ZksnarkException {
      this.d = d;
      this.pkD = pkD;
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
      validValueParams(value);
      validPositionParams(position);
      valid11Params(d);
      valid32Params(pkD);
      valid32Params(r);
      valid32Params(ak);
      valid32Params(nk);
      valid32Params(result);
    }
  }

  /**
   * diversifier: d, 11 bytes esk: 32 bytes result: return 32 bytes
   */
  public static class KaDerivepublicParams implements ValidParam {

    @Setter
    @Getter
    private byte[] diversifier;
    @Setter
    @Getter
    private byte[] esk;
    @Setter
    @Getter
    private byte[] result;

    public KaDerivepublicParams(byte[] diversifier, byte[] esk, byte[] result)
        throws ZksnarkException {
      this.diversifier = diversifier;
      this.esk = esk;
      this.result = result;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      valid11Params(diversifier);
      valid32Params(esk);
      valid32Params(result);
    }
  }

  /**
   * calculate spend proof ak: 32 bytes nsk: the proof authorizing key, 32 bytes d: 11 bytes r: rcm,
   * 32 bytes alpha: random number, 32 bytes anchor: 32 bytes voucherPath: (1 + 33 * 32 + 8) bytes,
   * voucherPath[0]=0x20, voucherPath[1+i*33]=0x20,i=0,1,...31. cv: value commitment, 32 bytes rk:
   * spendAuthSig.randomizePublicKey 32 bytes zkproof: spend proof, 192 bytes value >= 0
   */
  public static class SpendProofParams implements ValidParam {

    @Setter
    @Getter
    private long ctx;
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

    public SpendProofParams(long ctx, byte[] ak, byte[] nsk, byte[] d, byte[] r,
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

    public static SpendProofParams decode(long ctx, byte[] data)
        throws ZksnarkException {
      byte[] ak = new byte[32];
      byte[] nsk = new byte[32];
      byte[] d = new byte[11];
      byte[] r = new byte[32];
      byte[] alpha = new byte[32];
      byte[] valueByte = new byte[8];
      byte[] anchor = new byte[32];
      byte[] voucherPath = new byte[1065];
      byte[] cv = new byte[32];
      byte[] rk = new byte[192];
      byte[] zkproof = new byte[64];

      System.arraycopy(data, 0, ak, 0, 32);
      System.arraycopy(data, 32, nsk, 0, 32);
      System.arraycopy(data, 64, d, 0, 11);
      System.arraycopy(data, 75, r, 0, 32);
      System.arraycopy(data, 107, alpha, 0, 32);
      System.arraycopy(data, 139, valueByte, 0, 8);
      System.arraycopy(data, 147, anchor, 0, 32);
      System.arraycopy(data, 179, voucherPath, 0, 1065);
      System.arraycopy(data, 179 + 1065, cv, 0, 32);
      System.arraycopy(data, 211 + 1065, rk, 0, 192);
      System.arraycopy(data, 243 + 1065, zkproof, 0, 64);

      return new SpendProofParams(ctx, ak, nsk, d, r, alpha, ByteArray.toLong(valueByte),
          anchor, voucherPath, cv, rk, zkproof);
    }

    @Override
    public void valid() throws ZksnarkException {
      validValueParams(value);
      valid32Params(ak);
      valid32Params(nsk);
      valid11Params(d);
      valid32Params(r);
      valid32Params(alpha);
      valid32Params(anchor);
      validVoucherPath(voucherPath);
      valid32Params(cv);
      valid32Params(rk);
      validParamLength(zkproof, 192);
    }

    public byte[] encode() {
      byte[] data = new byte[32 + 32 + 11 + 32 + 32 + 8 + 32 + 1065 + 32 + 32 + 192];

      System.arraycopy(ak, 0, data, 0, 32);
      System.arraycopy(nsk, 0, data, 32, 32);
      System.arraycopy(d, 0, data, 64, 11);
      System.arraycopy(r, 0, data, 75, 32);
      System.arraycopy(alpha, 0, data, 107, 32);
      System.arraycopy(ByteArray.fromLong(value), 0, data, 139, 8);
      System.arraycopy(anchor, 0, data, 147, 32);
      System.arraycopy(voucherPath, 0, data, 179, 1065);
      System.arraycopy(cv, 0, data, 179 + 1065, 32);
      System.arraycopy(rk, 0, data, 211 + 1065, 32);
      System.arraycopy(zkproof, 0, data, 243 + 1065, 192);

      return data;
    }
  }

  /**
   * esk: 32 bytes d: 11 bytes pkD: 32 bytes r: rcm, 32 bytes cv: value commitment, 32 bytes
   * zkproof: receive proof, 192 bytes, value >= 0
   */
  public static class OutputProofParams implements ValidParam {

    @Setter
    @Getter
    private long ctx;
    @Setter
    @Getter
    private byte[] esk;
    @Setter
    @Getter
    private byte[] d;
    @Setter
    @Getter
    private byte[] pkD;
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

    public OutputProofParams(long ctx, byte[] esk, byte[] d, byte[] pkD, byte[] r,
        long value, byte[] cv, byte[] zkproof) throws ZksnarkException {
      this.ctx = ctx;
      this.esk = esk;
      this.d = d;
      this.pkD = pkD;
      this.r = r;
      this.value = value;
      this.cv = cv;
      this.zkproof = zkproof;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validValueParams(value);
      valid32Params(esk);
      valid11Params(d);
      valid32Params(pkD);
      valid32Params(r);
      valid32Params(cv);
      validParamLength(zkproof, 192);
    }
  }

  /**
   * ask: the spend authorizing key, 32 bytes alpha: random number, 32 bytes sigHash: sha256 of
   * transaction, 32 bytes result: spendAuthSig, 64 bytes
   */
  public static class SpendSigParams implements ValidParam {

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

    public SpendSigParams(byte[] ask, byte[] alpha, byte[] sigHash, byte[] result)
        throws ZksnarkException {
      this.ask = ask;
      this.alpha = alpha;
      this.sigHash = sigHash;
      this.result = result;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      valid32Params(ask);
      valid32Params(alpha);
      valid32Params(sigHash);
      validParamLength(result, 64);
    }
  }

  /**
   * Generate binding signature sighash: sha256 of transaction,32 bytes result: binding signature,
   * 64 bytes
   */
  public static class BindingSigParams implements ValidParam {

    @Setter
    @Getter
    private long ctx;
    @Setter
    @Getter
    private long valueBalance;
    @Setter
    @Getter
    private byte[] sighash;
    @Setter
    @Getter
    private byte[] result;

    public BindingSigParams(long ctx, long valueBalance, byte[] sighash, byte[] result)
        throws ZksnarkException {
      this.ctx = ctx;
      this.valueBalance = valueBalance;
      this.sighash = sighash;
      this.result = result;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      valid32Params(sighash);
      validParamLength(result, 64);
    }
  }

  /**
   * cv: value commitments, 32 bytes anchor: 32 bytes nullifier: 32 bytes rk:
   * spendAuthSig.randomizePublicKey, 32 bytes zkproof: spend proof, 192 bytes spendAuthSig: 64
   * bytes sighashValue: sha256 of transaction, 32 bytes
   */
  public static class CheckSpendParams implements ValidParam {

    @Setter
    @Getter
    private long ctx;
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

    public CheckSpendParams(long ctx, byte[] cv, byte[] anchor, byte[] nullifier,
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

    public static CheckSpendParams decode(long ctx, byte[] data, byte[] sigHashValue)
        throws ZksnarkException {
      byte[] cv = new byte[32];
      byte[] anchor = new byte[32];
      byte[] nullifier = new byte[32];
      byte[] rk = new byte[32];
      byte[] zkproof = new byte[192];
      byte[] spendAuthSig = new byte[64];

      System.arraycopy(data, 0, cv, 0, 32);
      System.arraycopy(data, 32, anchor, 0, 32);
      System.arraycopy(data, 64, nullifier, 0, 32);
      System.arraycopy(data, 96, rk, 0, 32);
      System.arraycopy(data, 128, zkproof, 0, 192);
      System.arraycopy(data, 320, spendAuthSig, 0, 64);

      return new CheckSpendParams(ctx, cv, anchor, nullifier, rk, zkproof, spendAuthSig,
          sigHashValue);
    }

    @Override
    public void valid() throws ZksnarkException {
      valid32Params(cv);
      valid32Params(anchor);
      valid32Params(nullifier);
      valid32Params(rk);
      validParamLength(zkproof, 192);
      validParamLength(spendAuthSig, 64);
      valid32Params(sighashValue);
    }
  }

  /**
   * cv: value commitments, 32 bytes cm: note commitment, 32 bytes ephemeralKey: 32 bytes zkproof:
   * 192 bytes
   */
  public static class CheckOutputParams implements ValidParam {

    @Setter
    @Getter
    private long ctx;
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

    public CheckOutputParams(long ctx, byte[] cv, byte[] cm, byte[] ephemeralKey,
        byte[] zkproof) throws ZksnarkException {
      this.ctx = ctx;
      this.cv = cv;
      this.cm = cm;
      this.ephemeralKey = ephemeralKey;
      this.zkproof = zkproof;
      valid();
    }

    public static CheckOutputParams decode(long ctx, byte[] data)
        throws ZksnarkException {
      byte[] cv = new byte[32];
      byte[] cm = new byte[32];
      byte[] ephemeralKey = new byte[32];
      byte[] zkproof = new byte[192];

      System.arraycopy(data, 0, cv, 0, 32);
      System.arraycopy(data, 32, cm, 0, 32);
      System.arraycopy(data, 64, ephemeralKey, 0, 32);
      System.arraycopy(data, 96, zkproof, 0, 192);

      return new CheckOutputParams(ctx, cv, cm, ephemeralKey, zkproof);
    }

    public static CheckOutputParams decodeZ(long ctx, byte[] data)
        throws ZksnarkException {
      byte[] cv = new byte[32];
      byte[] cm = new byte[32];
      byte[] ephemeralKey = new byte[32];
      byte[] zkproof = new byte[192];

      System.arraycopy(data, 0, cv, 0, 32);
      System.arraycopy(data, 32, cm, 0, 32);
      System.arraycopy(data, 64, ephemeralKey, 0, 32);
      System.arraycopy(data, 96 + 580 + 80, zkproof, 0, 192);

      return new CheckOutputParams(ctx, cv, cm, ephemeralKey, zkproof);
    }

    public byte[] encode() {
      byte[] data = new byte[32 + 32 + 32 + 192];
      System.arraycopy(cv, 0, data, 0, 32);
      System.arraycopy(cm, 0, data, 32, 32);
      System.arraycopy(ephemeralKey, 0, data, 64, 32);
      System.arraycopy(zkproof, 0, data, 96, 192);
      return data;
    }

    @Override
    public void valid() throws ZksnarkException {
      valid32Params(cv);
      valid32Params(cm);
      valid32Params(ephemeralKey);
      validParamLength(zkproof, 192);
    }
  }

  /**
   * bindingSig: 64 bytes sighashValue: sha256 of transaction,32 bytes
   */
  public static class FinalCheckParams implements ValidParam {

    @Setter
    @Getter
    private long ctx;
    @Setter
    @Getter
    private long valueBalance;
    @Setter
    @Getter
    private byte[] bindingSig;
    @Setter
    @Getter
    private byte[] sighashValue;

    public FinalCheckParams(long ctx, long valueBalance, byte[] bindingSig,
        byte[] sighashValue) throws ZksnarkException {
      this.ctx = ctx;
      this.valueBalance = valueBalance;
      this.bindingSig = bindingSig;
      this.sighashValue = sighashValue;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validParamLength(bindingSig, 64);
      valid32Params(sighashValue);
    }
  }

  /**
   * cv: value commitments, 32 bytes anchor: 32 bytes nullifier: 32 bytes rk:
   * spendAuthSig.randomizePublicKey, 32 bytes zkproof: spend proof, 192 bytes spendAuthSig: 64
   * bytes sighashValue: sha256 of transaction, 32 bytes
   */
  public static class CheckSpendNewParams implements ValidParam {

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

    public CheckSpendNewParams(byte[] cv, byte[] anchor, byte[] nullifier,
        byte[] rk, byte[] zkproof, byte[] spendAuthSig, byte[] sighashValue)
        throws ZksnarkException {
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
      valid32Params(cv);
      valid32Params(anchor);
      valid32Params(nullifier);
      valid32Params(rk);
      validParamLength(zkproof, 192);
      validParamLength(spendAuthSig, 64);
      valid32Params(sighashValue);
    }
  }

  /**
   * cv: value commitments, 32 bytes cm: note commitment, 32 bytes ephemeralKey: 32 bytes zkproof:
   * 192 bytes
   */
  public static class CheckOutputNewParams implements ValidParam {

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

    public CheckOutputNewParams(byte[] cv, byte[] cm, byte[] ephemeralKey, byte[] zkproof)
        throws ZksnarkException {
      this.cv = cv;
      this.cm = cm;
      this.ephemeralKey = ephemeralKey;
      this.zkproof = zkproof;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      valid32Params(cv);
      valid32Params(cm);
      valid32Params(ephemeralKey);
      validParamLength(zkproof, 192);
    }
  }

  /**
   * bindingSig: 64 bytes sighashValue: sha256 of transaction,32 bytes,
   */
  public static class FinalCheckNewParams implements ValidParam {

    @Setter
    @Getter
    private long valueBalance;
    @Setter
    @Getter
    private byte[] bindingSig;
    @Setter
    @Getter
    private byte[] sighashValue;
    @Setter
    @Getter
    private byte[] spendCv;
    @Setter
    @Getter
    private int spendCvLen;
    @Setter
    @Getter
    private byte[] outputCv;
    @Setter
    @Getter
    private int outputCvLen;

    public FinalCheckNewParams(long valueBalance, byte[] bindingSig, byte[] sighashValue,
        byte[] spendCv, int spendCvLen, byte[] outputCv, int outputCvLen) throws ZksnarkException {
      this.valueBalance = valueBalance;
      this.bindingSig = bindingSig;
      this.sighashValue = sighashValue;
      this.spendCv = spendCv;
      this.spendCvLen = spendCvLen;
      this.outputCv = outputCv;
      this.outputCvLen = outputCvLen;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validParamLength(bindingSig, 64);
      valid32Params(sighashValue);
      if (spendCvLen <= 0 || outputCvLen <= 0) {
        throw new ZksnarkException("spendCvLen and  outputCvLen must be positive");
      }
      if (spendCvLen % 32 != 0 || outputCvLen % 32 != 0) {
        throw new ZksnarkException(
            "spendCvLen and  ouFinalCheckNewParamstputCvLen must be multiple of 32");
      }
      validParamLength(spendCv, spendCvLen);
      validParamLength(outputCv, outputCvLen);
    }
  }

  /**
   * ivk: incoming viewing key, 32 bytes, should be 251bits , not checked; d: 11 bytes pkD: 32
   * bytes
   */
  public static class IvkToPkdParams implements ValidParam {

    @Setter
    @Getter
    private byte[] ivk;
    @Setter
    @Getter
    private byte[] d;
    @Setter
    @Getter
    private byte[] pkD;

    public IvkToPkdParams(byte[] ivk, byte[] d, byte[] pkD) throws ZksnarkException {
      this.ivk = ivk;
      this.d = d;
      this.pkD = pkD;
      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      valid32Params(ivk);
      valid11Params(d);
      valid32Params(pkD);
      if ((ivk[31] >> 3) != 0) {
        throw new ZksnarkException("Most significant five bits of ivk should be 0.");
      }
    }
  }

  /**
   * 0 <= depth < 63 a: 32 bytes b: 32 bytes result: 32 bytes
   */
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
      if (!((depth < 63) && (depth >= 0))) {
        throw new ZksnarkException("Merkle tree depth must be smaller than 63");
      }
      valid32Params(a);
      valid32Params(b);
      valid32Params(result);
    }
  }
}
