package org.tron.common.zksnark;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteUtil;
import org.tron.core.exception.ZksnarkException;

public class JLibsodiumParam {

  interface ValidParam {

    void valid() throws ZksnarkException;
  }

  public static void validNull(byte[] value) throws ZksnarkException {
    if (ByteUtil.isNullOrZeroArray(value)) {
      throw new ZksnarkException("param is null");
    }
  }

  public static void validValueParams(long value) throws ZksnarkException {
    if (value < 0) {
      throw new ZksnarkException("Value should be non-negative.");
    }
  }

  public static void validParamLength(byte[] value, int length) throws ZksnarkException {
    validNull(value);
    if (value.length != length) {
      throw new ZksnarkException("param length must be " + length);
    }
  }

  public static class Blake2bInitSaltPersonalParams implements ValidParam {

    @Setter
    @Getter
    private long state;
    @Setter
    @Getter
    private byte[] key;
    @Setter
    @Getter
    private int keyLen;
    @Setter
    @Getter
    private int outLen;
    @Setter
    @Getter
    private byte[] salt;
    @Setter
    @Getter
    private byte[] personal;

    public Blake2bInitSaltPersonalParams(long state, byte[] key, int keyLen, int outLen,
        byte[] salt, byte[] personal) throws ZksnarkException {
      this.state = state;
      this.key = key;
      this.keyLen = keyLen;
      this.outLen = outLen;
      this.salt = salt;
      this.personal = personal;

      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validValueParams(state);
      validParamLength(personal, 16);
    }
  }

  public static class Blake2bUpdateParams implements ValidParam {

    @Setter
    @Getter
    private long state;
    @Setter
    @Getter
    private byte[] in;
    @Setter
    @Getter
    private long inLen;

    public Blake2bUpdateParams(long state, byte[] in, long inLen) throws ZksnarkException {
      this.state = state;
      this.in = in;
      this.inLen = inLen;

      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validValueParams(state);
      if (in.length != inLen || (in.length != 33 && in.length != 34)) {
        throw new ZksnarkException("param length must be 33 or 34");
      }
    }
  }

  public static class Blake2bFinalParams implements ValidParam {

    @Setter
    @Getter
    private long state;
    @Setter
    @Getter
    private byte[] out;
    @Setter
    @Getter
    private int outLen;

    public Blake2bFinalParams(long state, byte[] out, int outLen) throws ZksnarkException {
      this.state = state;
      this.out = out;
      this.outLen = outLen;

      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validValueParams(state);
      if (out.length != outLen || (out.length != 11 && out.length != 64)) {
        throw new ZksnarkException("param length must be 11 or 64");
      }
    }
  }

  public static class Black2bSaltPersonalParams implements ValidParam {

    @Setter
    @Getter
    private byte[] out;
    @Setter
    @Getter
    private int outLen;
    @Setter
    @Getter
    private byte[] in;
    @Setter
    @Getter
    private long inLen;
    @Setter
    @Getter
    private byte[] key;
    @Setter
    @Getter
    private int keyLen;
    @Setter
    @Getter
    private byte[] salt;
    @Setter
    @Getter
    private byte[] personal;


    public Black2bSaltPersonalParams(byte[] out, int outLen, byte[] in, long inLen, byte[] key,
        int keyLen, byte[] salt, byte[] personal) throws ZksnarkException {
      this.out = out;
      this.outLen = outLen;
      this.in = in;
      this.inLen = inLen;
      this.key = key;
      this.keyLen = keyLen;
      this.salt = salt;
      this.personal = personal;

      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      if (out.length != outLen || in.length != inLen) {
        throw new ZksnarkException("out.length is not equal to outlen "
            + "or in.length is not equal to inlen");
      }
      validParamLength(out, 32);
      validParamLength(personal, 16);
    }
  }

  public static class Chacha20poly1305IetfDecryptParams implements ValidParam {

    @Setter
    @Getter
    private byte[] m;
    @Setter
    @Getter
    private long[] mLenP;
    @Setter
    @Getter
    private byte[] nSec;
    @Setter
    @Getter
    private byte[] c;
    @Setter
    @Getter
    private long cLen;
    @Setter
    @Getter
    private byte[] ad;
    @Setter
    @Getter
    private long adLen;
    @Setter
    @Getter
    private byte[] nPub;
    @Setter
    @Getter
    private byte[] k;

    public Chacha20poly1305IetfDecryptParams(byte[] m, long[] mLenP, byte[] nSec, byte[] c,
        long cLen, byte[] ad, long adLen, byte[] nPub, byte[] k) throws ZksnarkException {
      this.m = m;
      this.mLenP = mLenP;
      this.nSec = nSec;
      this.c = c;
      this.cLen = cLen;
      this.ad = ad;
      this.adLen = adLen;
      this.nPub = nPub;
      this.k = k;

      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validParamLength(nPub, 12);
      validParamLength(k, 32);
    }
  }

  public static class Chacha20Poly1305IetfEncryptParams implements ValidParam {

    @Setter
    @Getter
    private byte[] c;
    @Setter
    @Getter
    private long[] cLenP;
    @Setter
    @Getter
    private byte[] m;
    @Setter
    @Getter
    private long mLen;
    @Setter
    @Getter
    private byte[] ad;
    @Setter
    @Getter
    private long adLen;
    @Setter
    @Getter
    private byte[] nSec;
    @Setter
    @Getter
    private byte[] nPub;
    @Setter
    @Getter
    private byte[] k;

    public Chacha20Poly1305IetfEncryptParams(byte[] c, long[] cLenP, byte[] m, long mLen,
        byte[] ad, long adLen, byte[] nSec, byte[] nPub, byte[] k) throws ZksnarkException {
      this.c = c;
      this.cLenP = cLenP;
      this.m = m;
      this.mLen = mLen;
      this.ad = ad;
      this.adLen = adLen;
      this.nSec = nSec;
      this.nPub = nPub;
      this.k = k;

      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validParamLength(nPub, 12);
      validParamLength(k, 32);
    }
  }
}
