package org.tron.common.crypto.sm2;

import java.math.BigInteger;
import java.security.SecureRandom;
import javax.annotation.Nullable;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.SM3Digest;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECKeyParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.crypto.params.ParametersWithID;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.crypto.signers.DSAKCalculator;
import org.spongycastle.crypto.signers.RandomDSAKCalculator;
import org.spongycastle.math.ec.ECConstants;
import org.spongycastle.math.ec.ECFieldElement;
import org.spongycastle.math.ec.ECMultiplier;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.math.ec.FixedPointCombMultiplier;
import org.spongycastle.util.BigIntegers;

public class SM2Signer
    implements ECConstants {

  private final DSAKCalculator kCalculator = new RandomDSAKCalculator();

  private byte[] userID;

  private int curveLength;
  private ECDomainParameters ecParams;
  private ECPoint pubPoint;
  private ECKeyParameters ecKey;

  private SecureRandom random;

  public void init(boolean forSigning, CipherParameters param) {
    CipherParameters baseParam;

    if (param instanceof ParametersWithID) {
      baseParam = ((ParametersWithID) param).getParameters();
      userID = ((ParametersWithID) param).getID();
    } else {
      baseParam = param;
      userID = new byte[0];
    }

    if (forSigning) {
      if (baseParam instanceof ParametersWithRandom) {
        ParametersWithRandom rParam = (ParametersWithRandom) baseParam;

        ecKey = (ECKeyParameters) rParam.getParameters();
        ecParams = ecKey.getParameters();
        kCalculator.init(ecParams.getN(), rParam.getRandom());
      } else {
        ecKey = (ECKeyParameters) baseParam;
        ecParams = ecKey.getParameters();
        kCalculator.init(ecParams.getN(), new SecureRandom());
      }
      pubPoint = ecParams.getG().multiply(((ECPrivateKeyParameters) ecKey).getD()).normalize();
    } else {
      ecKey = (ECKeyParameters) baseParam;
      ecParams = ecKey.getParameters();
      pubPoint = ((ECPublicKeyParameters) ecKey).getQ();
    }

    curveLength = (ecParams.getCurve().getFieldSize() + 7) / 8;
  }


  /**
   * generate the signature for the message
   *
   * @param message plaintext
   */
  public BigInteger[] generateSignature(byte[] message) {
    byte[] eHash = generateSM3Hash(message);
    return generateHashSignature(eHash);
  }

  /**
   * generate the signature for the message
   */

  public byte[] generateSM3Hash(byte[] message) {
    //byte[] msg = message.getBytes();

    SM3Digest digest = new SM3Digest();
    byte[] z = getZ(digest);

    digest.update(z, 0, z.length);
    digest.update(message, 0, message.length);

    byte[] eHash = new byte[digest.getDigestSize()];

    digest.doFinal(eHash, 0);
    return eHash;
  }

  /**
   * generate the signature from the 32 byte hash
   */
  public BigInteger[] generateHashSignature(byte[] hash) {
    if (hash.length != 32) {
      throw new IllegalArgumentException("Expected 32 byte input to " +
          "ECDSA signature, not " + hash.length);
    }
    BigInteger n = ecParams.getN();
    BigInteger e = calculateE(hash);
    BigInteger d = ((ECPrivateKeyParameters) ecKey).getD();

    BigInteger r, s;

    ECMultiplier basePointMultiplier = createBasePointMultiplier();

    // 5.2.1 Draft RFC:  SM2 Public Key Algorithms
    do // generate s
    {
      BigInteger k;
      do // generate r
      {
        // A3
        k = kCalculator.nextK();
        // A4
        ECPoint p = basePointMultiplier.multiply(ecParams.getG(), k).normalize();

        // A5
        r = e.add(p.getAffineXCoord().toBigInteger()).mod(n);
      }
      while (r.equals(ZERO) || r.add(k).equals(n));

      // A6
      BigInteger dPlus1ModN = d.add(ONE).modInverse(n);

      s = k.subtract(r.multiply(d)).mod(n);
      s = dPlus1ModN.multiply(s).mod(n);
    }
    while (s.equals(ZERO));

    // A7
    return new BigInteger[]{r, s};
  }

  /**
   * verify the message signature
   */
  public boolean verifySignature(byte[] message, BigInteger r, BigInteger s,
      @Nullable String userID) {
    BigInteger n = ecParams.getN();

    // 5.3.1 Draft RFC:  SM2 Public Key Algorithms
    // B1
    if (r.compareTo(ONE) < 0 || r.compareTo(n) >= 0) {
      return false;
    }

    // B2
    if (s.compareTo(ONE) < 0 || s.compareTo(n) >= 0) {
      return false;
    }

    ECPoint q = ((ECPublicKeyParameters) ecKey).getQ();

    if (userID != null) {
      this.userID = userID.getBytes();
    }
    byte[] eHash = generateSM3Hash(message);

    // B4
    BigInteger e = calculateE(eHash);

    // B5
    BigInteger t = r.add(s).mod(n);
    if (t.equals(ZERO)) {
      return false;
    } else {
      // B6
      ECPoint x1y1 = ecParams.getG().multiply(s);
      x1y1 = x1y1.add(q.multiply(t)).normalize();

      // B7
      return r.equals(e.add(x1y1.getAffineXCoord().toBigInteger()).mod(n));
    }
  }

  /**
   * verfify the hash signature
   */
  public boolean verifyHashSignature(byte[] hash, BigInteger r, BigInteger s) {
    BigInteger n = ecParams.getN();

    // 5.3.1 Draft RFC:  SM2 Public Key Algorithms
    // B1
    if (r.compareTo(ONE) < 0 || r.compareTo(n) >= 0) {
      return false;
    }

    // B2
    if (s.compareTo(ONE) < 0 || s.compareTo(n) >= 0) {
      return false;
    }

    ECPoint q = ((ECPublicKeyParameters) ecKey).getQ();

    // B4
    BigInteger e = calculateE(hash);

    // B5
    BigInteger t = r.add(s).mod(n);
    if (t.equals(ZERO)) {
      return false;
    } else {
      // B6
      ECPoint x1y1 = ecParams.getG().multiply(s);
      x1y1 = x1y1.add(q.multiply(t)).normalize();

      // B7
      return r.equals(e.add(x1y1.getAffineXCoord().toBigInteger()).mod(n));
    }
  }

  private byte[] getZ(Digest digest) {

    //addUserID(digest, userID);

    addFieldElement(digest, ecParams.getCurve().getA());
    addFieldElement(digest, ecParams.getCurve().getB());
    addFieldElement(digest, ecParams.getG().getAffineXCoord());
    addFieldElement(digest, ecParams.getG().getAffineYCoord());
    addFieldElement(digest, pubPoint.getAffineXCoord());
    addFieldElement(digest, pubPoint.getAffineYCoord());

    byte[] rv = new byte[digest.getDigestSize()];

    digest.doFinal(rv, 0);

    return rv;
  }

  private void addUserID(Digest digest, byte[] userID) {
    int len = userID.length * 8;
    digest.update((byte) (len >> 8 & 0xFF));
    digest.update((byte) (len & 0xFF));
    digest.update(userID, 0, userID.length);
  }

  private void addFieldElement(Digest digest, ECFieldElement v) {
    byte[] p = BigIntegers.asUnsignedByteArray(curveLength, v.toBigInteger());
    digest.update(p, 0, p.length);
  }

  protected ECMultiplier createBasePointMultiplier() {
    return new FixedPointCombMultiplier();
  }

  protected BigInteger calculateE(byte[] message) {
    return new BigInteger(1, message);
  }

}

