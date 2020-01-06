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
    private int keylen;
    @Setter
    @Getter
    private int outlen;
    @Setter
    @Getter
    private byte[] salt;
    @Setter
    @Getter
    private byte[] personal;

    public Blake2bInitSaltPersonalParams(long state, byte[] key, int keylen, int outlen,
        byte[] salt, byte[] personal) throws ZksnarkException {
      this.state = state;
      this.key = key;
      this.keylen = keylen;
      this.outlen = outlen;
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
    private long inlen;

    public Blake2bUpdateParams(long state, byte[] in, long inlen) throws ZksnarkException {
      this.state = state;
      this.in = in;
      this.inlen = inlen;

      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validValueParams(state);
      if (in.length != 33 && in.length != 34) {
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
    private int outlen;

    public Blake2bFinalParams(long state, byte[] out, int outlen) throws ZksnarkException {
      this.state = state;
      this.out = out;
      this.outlen = outlen;

      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validValueParams(state);
      if (out.length != outlen || (out.length != 11 && out.length != 64)) {
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
    private int outlen;
    @Setter
    @Getter
    private byte[] in;
    @Setter
    @Getter
    private long inlen;
    @Setter
    @Getter
    private byte[] key;
    @Setter
    @Getter
    private int keylen;
    @Setter
    @Getter
    private byte[] salt;
    @Setter
    @Getter
    private byte[] personal;


    public Black2bSaltPersonalParams(byte[] out, int outlen, byte[] in, long inlen, byte[] key,
        int keylen, byte[] salt, byte[] personal) throws ZksnarkException {
      this.out = out;
      this.outlen = outlen;
      this.in = in;
      this.inlen = inlen;
      this.key = key;
      this.keylen = keylen;
      this.salt = salt;
      this.personal = personal;

      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      if(out.length != outlen || in.length != inlen){
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
    private long[] mlen_p;
    @Setter
    @Getter
    private byte[] nsec;
    @Setter
    @Getter
    private byte[] c;
    @Setter
    @Getter
    private long clen;
    @Setter
    @Getter
    private byte[] ad;
    @Setter
    @Getter
    private long adlen;
    @Setter
    @Getter
    private byte[] npub;
    @Setter
    @Getter
    private byte[] k;

    public Chacha20poly1305IetfDecryptParams(byte[] m, long[] mlen_p, byte[] nsec, byte[] c,
        long clen, byte[] ad, long adlen, byte[] npub, byte[] k) throws ZksnarkException {
      this.m = m;
      this.mlen_p = mlen_p;
      this.nsec = nsec;
      this.c = c;
      this.clen = clen;
      this.ad = ad;
      this.adlen = adlen;
      this.npub = npub;
      this.k = k;

      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validParamLength(npub, 12);
      validParamLength(k, 32);
    }
  }

  public static class Chacha20Poly1305IetfEncryptParams implements ValidParam {

    @Setter
    @Getter
    private byte[] c;
    @Setter
    @Getter
    private long[] clen_p;
    @Setter
    @Getter
    private byte[] m;
    @Setter
    @Getter
    private long mlen;
    @Setter
    @Getter
    private byte[] ad;
    @Setter
    @Getter
    private long adlen;
    @Setter
    @Getter
    private byte[] nsec;
    @Setter
    @Getter
    private byte[] npub;
    @Setter
    @Getter
    private byte[] k;

    public Chacha20Poly1305IetfEncryptParams(byte[] c, long[] clen_p, byte[] m, long mlen,
        byte[] ad, long adlen, byte[] nsec, byte[] npub, byte[] k) throws ZksnarkException {
      this.c = c;
      this.clen_p = clen_p;
      this.m = m;
      this.mlen = mlen;
      this.ad = ad;
      this.adlen = adlen;
      this.nsec = nsec;
      this.npub = npub;
      this.k = k;

      valid();
    }

    @Override
    public void valid() throws ZksnarkException {
      validParamLength(npub, 12);
      validParamLength(k, 32);
    }
  }
}
