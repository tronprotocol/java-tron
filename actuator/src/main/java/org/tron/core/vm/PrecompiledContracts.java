/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.vm;

import static org.tron.common.runtime.vm.DataWord.WORD_SIZE;
import static org.tron.common.utils.BIUtil.addSafely;
import static org.tron.common.utils.BIUtil.isLessThan;
import static org.tron.common.utils.BIUtil.isZero;
import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.tron.common.utils.ByteUtil.bytesToBigInteger;
import static org.tron.common.utils.ByteUtil.merge;
import static org.tron.common.utils.ByteUtil.numberOfLeadingZeros;
import static org.tron.common.utils.ByteUtil.parseBytes;
import static org.tron.common.utils.ByteUtil.parseWord;
import static org.tron.common.utils.ByteUtil.stripLeadingZeroes;
import static org.tron.core.db.TransactionTrace.convertToTronAddress;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.tron.common.crypto.SignUtils;
import org.tron.common.crypto.SignatureInterface;
import org.tron.common.crypto.zksnark.BN128;
import org.tron.common.crypto.zksnark.BN128Fp;
import org.tron.common.crypto.zksnark.BN128G1;
import org.tron.common.crypto.zksnark.BN128G2;
import org.tron.common.crypto.zksnark.Fp;
import org.tron.common.crypto.zksnark.PairingCheck;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.BIUtil;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol.Permission;

/**
 * @author Roman Mandeleil
 * @since 09.01.2015
 */

@Slf4j(topic = "VM")
public class PrecompiledContracts {

  private static final ECRecover ecRecover = new ECRecover();
  private static final Sha256 sha256 = new Sha256();
  private static final Ripempd160 ripempd160 = new Ripempd160();
  private static final Identity identity = new Identity();
  private static final ModExp modExp = new ModExp();
  private static final BN128Addition altBN128Add = new BN128Addition();
  private static final BN128Multiplication altBN128Mul = new BN128Multiplication();
  private static final BN128Pairing altBN128Pairing = new BN128Pairing();

  private static final BatchValidateSign batchValidateSign = new BatchValidateSign();
  private static final ValidateMultiSign validateMultiSign = new ValidateMultiSign();

  private static final VerifyMintProof verifyMintProof = new VerifyMintProof();
  private static final VerifyTransferProof verifyTransferProof = new VerifyTransferProof();
  private static final VerifyBurnProof verifyBurnProof = new VerifyBurnProof();

  private static final MerkleHash merkleHash = new MerkleHash();

  private static final DataWord ecRecoverAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000001");
  private static final DataWord sha256Addr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000002");
  private static final DataWord ripempd160Addr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000003");
  private static final DataWord identityAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000004");
  private static final DataWord modExpAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000005");
  private static final DataWord altBN128AddAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000006");
  private static final DataWord altBN128MulAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000007");
  private static final DataWord altBN128PairingAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000008");
  private static final DataWord batchValidateSignAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000009");
  private static final DataWord validateMultiSignAddr = new DataWord(
      "000000000000000000000000000000000000000000000000000000000000000a");
  private static final DataWord verifyMintProofAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000001000001");
  private static final DataWord verifyTransferProofAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000001000002");
  private static final DataWord verifyBurnProofAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000001000003");
  private static final DataWord merkleHashAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000001000004");

  public static PrecompiledContract getContractForAddress(DataWord address) {

    if (address == null) {
      return identity;
    }
    if (address.equals(ecRecoverAddr)) {
      return ecRecover;
    }
    if (address.equals(sha256Addr)) {
      return sha256;
    }
    if (address.equals(ripempd160Addr)) {
      return ripempd160;
    }
    if (address.equals(identityAddr)) {
      return identity;
    }
    // Byzantium precompiles
    if (address.equals(modExpAddr)) {
      return modExp;
    }
    if (address.equals(altBN128AddAddr)) {
      return altBN128Add;
    }
    if (address.equals(altBN128MulAddr)) {
      return altBN128Mul;
    }
    if (address.equals(altBN128PairingAddr)) {
      return altBN128Pairing;
    }
    if (VMConfig.allowTvmSolidity059() && address.equals(batchValidateSignAddr)) {
      return batchValidateSign;
    }
    if (VMConfig.allowTvmSolidity059() && address.equals(validateMultiSignAddr)) {
      return validateMultiSign;
    }
    if (VMConfig.allowShieldedTRC20Transaction() && address.equals(verifyMintProofAddr)) {
      return verifyMintProof;
    }
    if (VMConfig.allowShieldedTRC20Transaction() && address.equals(verifyTransferProofAddr)) {
      return verifyTransferProof;
    }
    if (VMConfig.allowShieldedTRC20Transaction() && address.equals(verifyBurnProofAddr)) {
      return verifyBurnProof;
    }
    if (VMConfig.allowShieldedTRC20Transaction() && address.equals(merkleHashAddr)) {
      return merkleHash;
    }

    return null;
  }

  private static byte[] encodeRes(byte[] w1, byte[] w2) {

    byte[] res = new byte[64];

    w1 = stripLeadingZeroes(w1);
    w2 = stripLeadingZeroes(w2);

    System.arraycopy(w1, 0, res, 32 - w1.length, w1.length);
    System.arraycopy(w2, 0, res, 64 - w2.length, w2.length);

    return res;
  }

  private static byte[] recoverAddrBySign(byte[] sign, byte[] hash) {
    byte v;
    byte[] r;
    byte[] s;
    byte[] out = null;
    if (ArrayUtils.isEmpty(sign) || sign.length < 65) {
      return new byte[0];
    }
    try {
      r = Arrays.copyOfRange(sign, 0, 32);
      s = Arrays.copyOfRange(sign, 32, 64);
      v = sign[64];
      if (v < 27) {
        v += 27;
      }

      SignatureInterface signature = SignUtils.fromComponents(r, s, v,
          CommonParameter.getInstance().isECKeyCryptoEngine());
      if (signature.validateComponents()) {
        out = SignUtils.signatureToAddress(hash, signature,
            CommonParameter.getInstance().isECKeyCryptoEngine());
      }
    } catch (Throwable any) {
      logger.info("ECRecover error", any.getMessage());
    }
    return out;
  }

  private static byte[][] extractBytes32Array(DataWord[] words, int offset) {
    int len = words[offset].intValueSafe();
    byte[][] bytes32Array = new byte[len][];
    for (int i = 0; i < len; i++) {
      bytes32Array[i] = words[offset + i + 1].getData();
    }
    return bytes32Array;
  }

  private static byte[][] extractBytesArray(DataWord[] words, int offset, byte[] data) {
    if (offset > words.length - 1) {
      return new byte[0][];
    }
    int len = words[offset].intValueSafe();
    byte[][] bytesArray = new byte[len][];
    for (int i = 0; i < len; i++) {
      int bytesOffset = words[offset + i + 1].intValueSafe() / WORD_SIZE;
      int bytesLen = words[offset + bytesOffset + 1].intValueSafe();
      bytesArray[i] = extractBytes(data, (bytesOffset + offset + 2) * WORD_SIZE,
          bytesLen);
    }
    return bytesArray;
  }

  private static byte[] extractBytes(byte[] data, int offset, int len) {
    return Arrays.copyOfRange(data, offset, offset + len);
  }

  public static abstract class PrecompiledContract {

    protected static final byte[] DATA_FALSE = new byte[WORD_SIZE];
    private byte[] callerAddress;
    private Repository deposit;
    private ProgramResult result;
    @Setter
    @Getter
    private boolean isConstantCall;
    @Getter
    @Setter
    private long vmShouldEndInUs;

    public abstract long getEnergyForData(byte[] data);

    public abstract Pair<Boolean, byte[]> execute(byte[] data);

    public void setRepository(Repository deposit) {
      this.deposit = deposit;
    }

    public byte[] getCallerAddress() {
      return callerAddress.clone();
    }

    public void setCallerAddress(byte[] callerAddress) {
      this.callerAddress = callerAddress.clone();
    }

    public Repository getDeposit() {
      return deposit;
    }

    public ProgramResult getResult() {
      return result;
    }

    public void setResult(ProgramResult result) {
      this.result = result;
    }

    protected long getCPUTimeLeftInNanoSecond() {
      long left = getVmShouldEndInUs() * VMConstant.ONE_THOUSAND - System.nanoTime();
      if (left <= 0) {
        throw Program.Exception.notEnoughTime("call");
      } else {
        return left;
      }
    }

    protected byte[] dataOne() {
      byte[] ret = new byte[WORD_SIZE];
      ret[31] = 1;
      return ret;
    }

    protected byte[] dataBoolean(boolean result) {
      if (result) {
        return DataWord.ONE().getData();
      }
      return DataWord.ZERO().getData();
    }
  }

  public static class Identity extends PrecompiledContract {

    public Identity() {
    }

    @Override
    public long getEnergyForData(byte[] data) {

      // energy charge for the execution:
      // minimum 1 and additional 1 for each 32 bytes word (round  up)
      if (data == null) {
        return 15;
      }
      return 15L + (data.length + 31) / 32 * 3;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      return Pair.of(true, data);
    }
  }

  public static class Sha256 extends PrecompiledContract {


    @Override
    public long getEnergyForData(byte[] data) {

      // energy charge for the execution:
      // minimum 50 and additional 50 for each 32 bytes word (round  up)
      if (data == null) {
        return 60;
      }
      return 60L + (data.length + 31) / 32 * 12;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      if (data == null) {
        return Pair.of(true, Sha256Hash.hash(CommonParameter
            .getInstance().isECKeyCryptoEngine(), EMPTY_BYTE_ARRAY));
      }
      return Pair.of(true, Sha256Hash.hash(CommonParameter
          .getInstance().isECKeyCryptoEngine(), data));
    }
  }

  public static class Ripempd160 extends PrecompiledContract {


    @Override
    public long getEnergyForData(byte[] data) {

      // TODO #POC9 Replace magic numbers with constants
      // energy charge for the execution:
      // minimum 50 and additional 50 for each 32 bytes word (round  up)
      if (data == null) {
        return 600;
      }
      return 600L + (data.length + 31) / 32 * 120;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      byte[] target = new byte[20];
      if (data == null) {
        data = EMPTY_BYTE_ARRAY;
      }

      byte[] orig = Sha256Hash.hash(CommonParameter.getInstance()
          .isECKeyCryptoEngine(), data);
      System.arraycopy(orig, 0, target, 0, 20);
      return Pair.of(true, Sha256Hash.hash(CommonParameter.getInstance()
          .isECKeyCryptoEngine(), target));
    }
  }

  public static class ECRecover extends PrecompiledContract {

    private static boolean validateV(byte[] v) {
      for (int i = 0; i < v.length - 1; i++) {
        if (v[i] != 0) {
          return false;
        }
      }
      return true;
    }

    @Override
    public long getEnergyForData(byte[] data) {
      return 3000;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      byte[] h = new byte[32];
      byte[] v = new byte[32];
      byte[] r = new byte[32];
      byte[] s = new byte[32];

      DataWord out = null;

      try {
        System.arraycopy(data, 0, h, 0, 32);
        System.arraycopy(data, 32, v, 0, 32);
        System.arraycopy(data, 64, r, 0, 32);

        int sLength = data.length < 128 ? data.length - 96 : 32;
        System.arraycopy(data, 96, s, 0, sLength);

        SignatureInterface signature = SignUtils.fromComponents(r, s, v[31]
            , CommonParameter.getInstance().isECKeyCryptoEngine());
        if (validateV(v) && signature.validateComponents()) {
          out = new DataWord(SignUtils.signatureToAddress(h, signature
              , CommonParameter.getInstance().isECKeyCryptoEngine()));
        }
      } catch (Throwable any) {
      }

      if (out == null) {
        return Pair.of(true, EMPTY_BYTE_ARRAY);
      } else {
        return Pair.of(true, out.getData());
      }
    }
  }

  /**
   * Computes modular exponentiation on big numbers
   * <p>
   * format of data[] array: [length_of_BASE] [length_of_EXPONENT] [length_of_MODULUS] [BASE]
   * [EXPONENT] [MODULUS] where every length is a 32-byte left-padded integer representing the
   * number of bytes. Call data is assumed to be infinitely right-padded with zero bytes.
   * <p>
   * Returns an output as a byte array with the same length as the modulus
   */
  public static class ModExp extends PrecompiledContract {

    private static final BigInteger GQUAD_DIVISOR = BigInteger.valueOf(20);

    private static final int ARGS_OFFSET = 32 * 3; // addresses length part

    @Override
    public long getEnergyForData(byte[] data) {

      if (data == null) {
        data = EMPTY_BYTE_ARRAY;
      }

      int baseLen = parseLen(data, 0);
      int expLen = parseLen(data, 1);
      int modLen = parseLen(data, 2);

      byte[] expHighBytes = parseBytes(data, addSafely(ARGS_OFFSET, baseLen), Math.min(expLen, 32));

      long multComplexity = getMultComplexity(Math.max(baseLen, modLen));
      long adjExpLen = getAdjustedExponentLength(expHighBytes, expLen);

      // use big numbers to stay safe in case of overflow
      BigInteger energy = BigInteger.valueOf(multComplexity)
          .multiply(BigInteger.valueOf(Math.max(adjExpLen, 1)))
          .divide(GQUAD_DIVISOR);

      return isLessThan(energy, BigInteger.valueOf(Long.MAX_VALUE)) ? energy.longValueExact()
          : Long.MAX_VALUE;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      if (data == null) {
        return Pair.of(true, EMPTY_BYTE_ARRAY);
      }

      int baseLen = parseLen(data, 0);
      int expLen = parseLen(data, 1);
      int modLen = parseLen(data, 2);

      BigInteger base = parseArg(data, ARGS_OFFSET, baseLen);
      BigInteger exp = parseArg(data, addSafely(ARGS_OFFSET, baseLen), expLen);
      BigInteger mod = parseArg(data, addSafely(addSafely(ARGS_OFFSET, baseLen), expLen), modLen);

      // check if modulus is zero
      if (isZero(mod)) {
        return Pair.of(true, EMPTY_BYTE_ARRAY);
      }

      byte[] res = stripLeadingZeroes(base.modPow(exp, mod).toByteArray());

      // adjust result to the same length as the modulus has
      if (res.length < modLen) {

        byte[] adjRes = new byte[modLen];
        System.arraycopy(res, 0, adjRes, modLen - res.length, res.length);

        return Pair.of(true, adjRes);

      } else {
        return Pair.of(true, res);
      }
    }

    private long getMultComplexity(long x) {

      long x2 = x * x;

      if (x <= 64) {
        return x2;
      }
      if (x <= 1024) {
        return x2 / 4 + 96 * x - 3072;
      }

      return x2 / 16 + 480 * x - 199680;
    }

    private long getAdjustedExponentLength(byte[] expHighBytes, long expLen) {

      int leadingZeros = numberOfLeadingZeros(expHighBytes);
      int highestBit = 8 * expHighBytes.length - leadingZeros;

      // set index basement to zero
      if (highestBit > 0) {
        highestBit--;
      }

      if (expLen <= 32) {
        return highestBit;
      } else {
        return 8 * (expLen - 32) + highestBit;
      }
    }

    private int parseLen(byte[] data, int idx) {
      byte[] bytes = parseBytes(data, 32 * idx, 32);
      return new DataWord(bytes).intValueSafe();
    }

    private BigInteger parseArg(byte[] data, int offset, int len) {
      byte[] bytes = parseBytes(data, offset, len);
      return bytesToBigInteger(bytes);
    }
  }

  /**
   * Computes point addition on Barreto–Naehrig curve. See {@link BN128Fp} for details<br/> <br/>
   * <p>
   * input data[]:<br/> two points encoded as (x, y), where x and y are 32-byte left-padded
   * integers,<br/> if input is shorter than expected, it's assumed to be right-padded with zero
   * bytes<br/> <br/>
   * <p>
   * output:<br/> resulting point (x', y'), where x and y encoded as 32-byte left-padded
   * integers<br/>
   */
  public static class BN128Addition extends PrecompiledContract {

    @Override
    public long getEnergyForData(byte[] data) {
      if (VMConfig.allowTvmIstanbul()) {
        return getEnergyForDataIstanbul(data);
      }
      return 500;
    }

    private long getEnergyForDataIstanbul(byte[] data) {
      return 150;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      if (data == null) {
        data = EMPTY_BYTE_ARRAY;
      }

      byte[] x1 = parseWord(data, 0);
      byte[] y1 = parseWord(data, 1);

      byte[] x2 = parseWord(data, 2);
      byte[] y2 = parseWord(data, 3);

      BN128<Fp> p1 = BN128Fp.create(x1, y1);
      if (p1 == null) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      BN128<Fp> p2 = BN128Fp.create(x2, y2);
      if (p2 == null) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      BN128<Fp> res = p1.add(p2).toEthNotation();

      return Pair.of(true, encodeRes(res.x().bytes(), res.y().bytes()));
    }
  }

  /**
   * Computes multiplication of scalar value on a point belonging to Barreto–Naehrig curve. See
   * {@link BN128Fp} for details<br/> <br/>
   * <p>
   * input data[]:<br/> point encoded as (x, y) is followed by scalar s, where x, y and s are
   * 32-byte left-padded integers,<br/> if input is shorter than expected, it's assumed to be
   * right-padded with zero bytes<br/> <br/>
   * <p>
   * output:<br/> resulting point (x', y'), where x and y encoded as 32-byte left-padded
   * integers<br/>
   */
  public static class BN128Multiplication extends PrecompiledContract {

    @Override
    public long getEnergyForData(byte[] data) {
      if (VMConfig.allowTvmIstanbul()) {
        return getEnergyForDataIstanbul(data);
      }
      return 40000;
    }

    private long getEnergyForDataIstanbul(byte[] data) {
      return 6000;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      if (data == null) {
        data = EMPTY_BYTE_ARRAY;
      }

      byte[] x = parseWord(data, 0);
      byte[] y = parseWord(data, 1);

      byte[] s = parseWord(data, 2);

      BN128<Fp> p = BN128Fp.create(x, y);
      if (p == null) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      BN128<Fp> res = p.mul(BIUtil.toBI(s)).toEthNotation();

      return Pair.of(true, encodeRes(res.x().bytes(), res.y().bytes()));
    }
  }

  /**
   * Computes pairing check. <br/> See {@link PairingCheck} for details.<br/> <br/>
   * <p>
   * Input data[]: <br/> an array of points (a1, b1, ... , ak, bk), <br/> where "ai" is a point of
   * {@link BN128Fp} curve and encoded as two 32-byte left-padded integers (x; y) <br/> "bi" is a
   * point of {@link BN128G2} curve and encoded as four 32-byte left-padded integers {@code (ai + b;
   * ci + d)}, each coordinate of the point is a big-endian {@link } number, so {@code b} precedes
   * {@code a} in the encoding: {@code (b, a; d, c)} <br/> thus each pair (ai, bi) has 192 bytes
   * length, if 192 is not a multiple of {@code data.length} then execution fails <br/> the number
   * of pairs is derived from input length by dividing it by 192 (the length of a pair) <br/> <br/>
   * <p>
   * output: <br/> pairing product which is either 0 or 1, encoded as 32-byte left-padded integer
   * <br/>
   */
  public static class BN128Pairing extends PrecompiledContract {

    private static final int PAIR_SIZE = 192;

    @Override
    public long getEnergyForData(byte[] data) {
      if (VMConfig.allowTvmIstanbul()) {
        return getEnergyForDataIstanbul(data);
      }
      if (data == null) {
        return 100000;
      }
      return 80000L * (data.length / PAIR_SIZE) + 100000;
    }

    private long getEnergyForDataIstanbul(byte[] data) {
      if (data == null) {
        return 45000;
      }
      return 34000L * (data.length / PAIR_SIZE) + 45000;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      if (data == null) {
        data = EMPTY_BYTE_ARRAY;
      }

      // fail if input len is not a multiple of PAIR_SIZE
      if (data.length % PAIR_SIZE > 0) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      PairingCheck check = PairingCheck.create();

      // iterating over all pairs
      for (int offset = 0; offset < data.length; offset += PAIR_SIZE) {

        Pair<BN128G1, BN128G2> pair = decodePair(data, offset);

        // fail if decoding has failed
        if (pair == null) {
          return Pair.of(false, EMPTY_BYTE_ARRAY);
        }

        check.addPair(pair.getLeft(), pair.getRight());
      }

      check.run();
      int result = check.result();

      return Pair.of(true, new DataWord(result).getData());
    }

    private Pair<BN128G1, BN128G2> decodePair(byte[] in, int offset) {

      byte[] x = parseWord(in, offset, 0);
      byte[] y = parseWord(in, offset, 1);

      BN128G1 p1 = BN128G1.create(x, y);

      // fail if point is invalid
      if (p1 == null) {
        return null;
      }

      // (b, a)
      byte[] b = parseWord(in, offset, 2);
      byte[] a = parseWord(in, offset, 3);

      // (d, c)
      byte[] d = parseWord(in, offset, 4);
      byte[] c = parseWord(in, offset, 5);

      BN128G2 p2 = BN128G2.create(a, b, c, d);

      // fail if point is invalid
      if (p2 == null) {
        return null;
      }

      return Pair.of(p1, p2);
    }
  }

  public static class ValidateMultiSign extends PrecompiledContract {

    private static final int ENGERYPERSIGN = 1500;
    private static final int MAX_SIZE = 5;


    @Override
    public long getEnergyForData(byte[] data) {
      int cnt = (data.length / WORD_SIZE - 5) / 5;
      // one sign 1500, half of ecrecover
      return (long) (cnt * ENGERYPERSIGN);
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] rawData) {
      DataWord[] words = DataWord.parseArray(rawData);
      byte[] addr = words[0].getLast20Bytes();
      int permissionId = words[1].intValueSafe();
      byte[] data = words[2].getData();

      byte[] combine = ByteUtil
          .merge(convertToTronAddress(addr), ByteArray.fromInt(permissionId), data);
      byte[] hash = Sha256Hash.hash(CommonParameter
          .getInstance().isECKeyCryptoEngine(), combine);

      byte[][] signatures = extractBytesArray(
          words, words[3].intValueSafe() / WORD_SIZE, rawData);

      if (signatures.length == 0 || signatures.length > MAX_SIZE) {
        return Pair.of(true, DATA_FALSE);
      }

      AccountCapsule account = this.getDeposit().getAccount(convertToTronAddress(addr));
      if (account != null) {
        try {
          Permission permission = account.getPermissionById(permissionId);
          if (permission != null) {
            //calculate weight
            long totalWeight = 0L;
            List<byte[]> executedSignList = new ArrayList<>();
            for (byte[] sign : signatures) {
              if (ByteArray.matrixContains(executedSignList, sign)) {
                continue;
              }
              byte[] recoveredAddr = recoverAddrBySign(sign, hash);
              long weight = TransactionCapsule.getWeight(permission, recoveredAddr);
              if (weight == 0) {
                //incorrect sign
                return Pair.of(true, DATA_FALSE);
              }
              totalWeight += weight;
              executedSignList.add(sign);
            }

            if (totalWeight >= permission.getThreshold()) {
              return Pair.of(true, dataOne());
            }
          }
        } catch (Throwable t) {
          logger.info("ValidateMultiSign error:{}", t.getMessage());
        }
      }
      return Pair.of(true, DATA_FALSE);
    }
  }

  public static class BatchValidateSign extends PrecompiledContract {

    private static final ExecutorService workers;
    private static final int ENGERYPERSIGN = 1500;
    private static final int MAX_SIZE = 16;

    static {
      workers = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
    }

    @Override
    public long getEnergyForData(byte[] data) {
      int cnt = (data.length / WORD_SIZE - 5) / 6;
      // one sign 1500, half of ecrecover
      return (long) (cnt * ENGERYPERSIGN);
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      try {
        return doExecute(data);
      } catch (Throwable t) {
        return Pair.of(true, new byte[WORD_SIZE]);
      }
    }

    private Pair<Boolean, byte[]> doExecute(byte[] data)
        throws InterruptedException, ExecutionException {
      DataWord[] words = DataWord.parseArray(data);
      byte[] hash = words[0].getData();
      byte[][] signatures = extractBytesArray(
          words, words[1].intValueSafe() / WORD_SIZE, data);
      byte[][] addresses = extractBytes32Array(
          words, words[2].intValueSafe() / WORD_SIZE);
      int cnt = signatures.length;
      if (cnt == 0 || cnt > MAX_SIZE || signatures.length != addresses.length) {
        return Pair.of(true, DATA_FALSE);
      }
      byte[] res = new byte[WORD_SIZE];
      if (isConstantCall()) {
        //for constant call not use thread pool to avoid potential effect
        for (int i = 0; i < cnt; i++) {
          if (DataWord
              .equalAddressByteArray(addresses[i], recoverAddrBySign(signatures[i], hash))) {
            res[i] = 1;
          }
        }
      } else {
        // add check
        CountDownLatch countDownLatch = new CountDownLatch(cnt);
        List<Future<RecoverAddrResult>> futures = new ArrayList<>(cnt);

        for (int i = 0; i < cnt; i++) {
          Future<RecoverAddrResult> future = workers
              .submit(new RecoverAddrTask(countDownLatch, hash, signatures[i], i));
          futures.add(future);
        }
        boolean withNoTimeout = countDownLatch
            .await(getCPUTimeLeftInNanoSecond(), TimeUnit.NANOSECONDS);

        if (!withNoTimeout) {
          logger.info("BatchValidateSign timeout");
          throw Program.Exception.notEnoughTime("call BatchValidateSign precompile method");
        }

        for (Future<RecoverAddrResult> future : futures) {
          RecoverAddrResult result = future.get();
          int index = result.nonce;
          if (DataWord.equalAddressByteArray(result.addr, addresses[index])) {
            res[index] = 1;
          }
        }
      }
      return Pair.of(true, res);
    }

    @AllArgsConstructor
    private static class RecoverAddrTask implements Callable<RecoverAddrResult> {

      private CountDownLatch countDownLatch;
      private byte[] hash;
      private byte[] signature;
      private int nonce;

      @Override
      public RecoverAddrResult call() {
        try {
          return new RecoverAddrResult(recoverAddrBySign(this.signature, this.hash), nonce);
        } finally {
          countDownLatch.countDown();
        }
      }
    }

    @AllArgsConstructor
    private static class RecoverAddrResult {

      private byte[] addr;
      private int nonce;
    }


  }

  public abstract static class VerifyProof extends PrecompiledContract {

    protected static final long TREE_WIDTH = 1L << 32;
    protected static final byte[][] UNCOMMITTED = new byte[32][32];

    static {
      UNCOMMITTED[0] = ByteArray.fromHexString(
          "0100000000000000000000000000000000000000000000000000000000000000");
      try {
        for (int i = 0; i < 31; i++) {
          JLibrustzcash.librustzcashMerkleHash(
              new LibrustzcashParam.MerkleHashParams(
                  i, UNCOMMITTED[i], UNCOMMITTED[i], UNCOMMITTED[i + 1]));
        }
      } catch (Throwable any) {
        logger.info("Initialize UNCOMMITTED array failed:{}", any.getMessage());
      }
    }

    protected long parseLong(byte[] data, int idx) {
      byte[] bytes = parseBytes(data, idx, 32);
      return new DataWord(bytes).longValueSafe();
    }

    protected int parseInt(byte[] data, int idx) {
      byte[] bytes = parseBytes(data, idx, 32);
      return new DataWord(bytes).intValueSafe();
    }

    private int getFrontierSlot(long leafIndex) {
      int slot = 0;
      if (leafIndex % 2 != 0) {
        int exp1 = 1;
        long pow1 = 2;
        long pow2 = pow1 << 1;
        while (slot == 0) {
          if ((leafIndex + 1 - pow1) % pow2 == 0) {
            slot = exp1;
          } else {
            pow1 = pow2;
            pow2 = pow2 << 1;
            exp1++;
          }
        }
      }
      return slot;
    }

    protected Pair<Boolean, byte[]> insertLeaves(
        byte[][] frontier, long leafCount, byte[][] leafValue) {
      long nodeIndex = 0;
      boolean success = true;
      byte[] leftInput;
      byte[] rightInput;
      byte[] hash = new byte[32];
      byte[] nodeValue = new byte[32];
      int cmCount = leafValue.length;
      int[] slot = new int[cmCount];
      for (int i = 0; i < cmCount; i++) {
        slot[i] = getFrontierSlot(leafCount + i);
      }
      int resultArrayLength = 32;
      for (int i = 0; i < cmCount; i++) {
        resultArrayLength += (slot[i] + 1) * 32;
      }

      byte[] result = new byte[resultArrayLength];
      try {
        int offset = 0;
        for (int i = 0; i < cmCount; i++) {
          byte[] slotArray = DataWord.of((byte) (slot[i] & 0xFF)).getData();
          System.arraycopy(slotArray, 0, result, offset, 32);
          offset += 32;
          nodeIndex = i + leafCount + TREE_WIDTH - 1;
          System.arraycopy(leafValue[i], 0, nodeValue, 0, 32);
          if (slot[i] == 0) {
            System.arraycopy(nodeValue, 0, frontier[0], 0, 32);
            continue;
          }
          for (int level = 1; level <= slot[i]; level++) {
            if (nodeIndex % 2 == 0) {
              leftInput = frontier[level - 1];
              rightInput = nodeValue;
              nodeIndex = (nodeIndex - 1) / 2;
            } else {
              leftInput = nodeValue;
              rightInput = UNCOMMITTED[level - 1];
              nodeIndex = nodeIndex / 2;
            }
            JLibrustzcash.librustzcashMerkleHash(new LibrustzcashParam.MerkleHashParams(
                level - 1, leftInput, rightInput, hash));
            System.arraycopy(hash, 0, nodeValue, 0, 32);
            System.arraycopy(hash, 0, result, offset, 32);
            offset += 32;
          }
          System.arraycopy(nodeValue, 0, frontier[slot[i]], 0, 32);
        }

        for (int level = slot[cmCount - 1] + 1; level <= 32; level++) {
          if (nodeIndex % 2 == 0) {
            leftInput = frontier[level - 1];
            rightInput = nodeValue;
            nodeIndex = (nodeIndex - 1) / 2;
          } else {
            leftInput = nodeValue;
            rightInput = UNCOMMITTED[level - 1];
            nodeIndex = nodeIndex / 2;
          }
          JLibrustzcash.librustzcashMerkleHash(new LibrustzcashParam.MerkleHashParams(
              level - 1, leftInput, rightInput, hash));
          System.arraycopy(hash, 0, nodeValue, 0, 32);
        }
        System.arraycopy(nodeValue, 0, result, offset, 32);
      } catch (Throwable any) {
        success = false;
        String errorMsg = any.getMessage();
        if (errorMsg == null && any.getCause() != null) {
          errorMsg = any.getCause().getMessage();
        }
        logger.info("Insert leaves failed: " + errorMsg);
      }
      if (success) {
        return Pair.of(true, merge(DataWord.ONE().getData(), result));
      } else {
        return Pair.of(true, DataWord.ZERO().getData());
      }
    }
  }

  public static class VerifyMintProof extends VerifyProof {

    private static final int SIZE = 1504;

    @Override
    public long getEnergyForData(byte[] data) {
      return 150000;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      if (data == null) {
        return Pair.of(true, DataWord.ZERO().getData());
      }
      if (data.length != SIZE) {
        return Pair.of(true, DataWord.ZERO().getData());
      }
      boolean result;
      long ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
      try {
        byte[] cm = new byte[32];
        byte[] cv = new byte[32];
        byte[] epk = new byte[32];
        byte[] proof = new byte[192];
        byte[] bindingSig = new byte[64];
        byte[] signHash = new byte[32];
        byte[][] frontier = new byte[33][32];

        System.arraycopy(data, 0, cm, 0, 32);
        System.arraycopy(data, 32, cv, 0, 32);
        System.arraycopy(data, 64, epk, 0, 32);
        System.arraycopy(data, 96, proof, 0, 192);
        System.arraycopy(data, 288, bindingSig, 0, 64);
        long value = parseLong(data, 352);
        System.arraycopy(data, 384, signHash, 0, 32);
        for (int i = 0; i < 33; i++) {
          System.arraycopy(data, i * 32 + 416, frontier[i], 0, 32);
        }
        long leafCount = parseLong(data, 1472);
        if (leafCount >= TREE_WIDTH) {
          return Pair.of(true, DataWord.ZERO().getData());
        }

        result = JLibrustzcash.librustzcashSaplingCheckOutput(
            new LibrustzcashParam.CheckOutputParams(ctx, cv, cm, epk, proof));
        long valueBalance = -value;
        result = result && JLibrustzcash.librustzcashSaplingFinalCheck(
            new LibrustzcashParam.FinalCheckParams(ctx, valueBalance, bindingSig, signHash));

        if (result) {
          byte[][] leafValue = new byte[1][32];
          System.arraycopy(cm, 0, leafValue[0], 0, 32);
          return insertLeaves(frontier, leafCount, leafValue);
        } else {
          return Pair.of(true, DataWord.ZERO().getData());
        }
      } catch (Throwable any) {
        String errorMsg = any.getMessage();
        if (errorMsg == null && any.getCause() != null) {
          errorMsg = any.getCause().getMessage();
        }
        logger.info("VerifyMintProof exception " + errorMsg);
      } finally {
        JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
      }
      return Pair.of(true, DataWord.ZERO().getData());
    }
  }

  public static class VerifyTransferProof extends VerifyProof {

    private static final Integer[] SIZE = {2080, 2368, 2464, 2752};
    private static final ExecutorService workersInConstantCall;
    private static final ExecutorService workersInNonConstantCall;

    static {
      workersInConstantCall = Executors.newFixedThreadPool(5);
      workersInNonConstantCall = Executors.newFixedThreadPool(5);
    }

    @Override
    public long getEnergyForData(byte[] data) {
      return 200000;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      if (data == null) {
        return Pair.of(true, DataWord.ZERO().getData());
      }
      if (!Arrays.asList(SIZE).contains(data.length)) {
        return Pair.of(true, DataWord.ZERO().getData());
      }
      try {
        byte[] bindingSig = new byte[64];
        byte[] signHash = new byte[32];
        byte[][] frontier = new byte[33][32];
        //parse unfixed field offset
        int spendOffset = parseInt(data, 0);
        int spendAuthSigOffset = parseInt(data, 32);
        int receiveOffset = parseInt(data, 64);
        System.arraycopy(data, 96, bindingSig, 0, 64);
        System.arraycopy(data, 160, signHash, 0, 32);
        //parse value
        long value = parseLong(data, 192);
        for (int i = 0; i < 33; i++) {
          System.arraycopy(data, i * 32 + 224, frontier[i], 0, 32);
        }
        long leafCount = parseLong(data, 1280);
        if (leafCount >= TREE_WIDTH - 1) {
          return Pair.of(true, DataWord.ZERO().getData());
        }

        int spendCount = parseInt(data, spendOffset);
        int spendAuthSigCount = parseInt(data, spendAuthSigOffset);
        int receiveCount = parseInt(data, receiveOffset);

        if (spendCount != spendAuthSigCount || spendCount < 1
            || spendCount > 2 || receiveCount < 1 || receiveCount > 2) {
          return Pair.of(true, DataWord.ZERO().getData());
        }
        byte[][] anchor = new byte[spendCount][32];
        byte[][] nullifier = new byte[spendCount][32];
        byte[][] spendCv = new byte[spendCount][32];
        byte[][] rk = new byte[spendCount][32];
        byte[][] spendProof = new byte[spendCount][192];
        byte[][] spendAuthSig = new byte[spendCount][64];
        byte[][] receiveCm = new byte[receiveCount][32];
        byte[][] receiveCv = new byte[receiveCount][32];
        byte[][] receiveEpk = new byte[receiveCount][32];
        byte[][] receiveProof = new byte[receiveCount][192];

        //spend
        spendOffset += 32;
        for (int i = 0; i < spendCount; i++) {
          System.arraycopy(data, spendOffset + 320 * i, nullifier[i], 0, 32);
          System.arraycopy(data, spendOffset + 320 * i + 32, anchor[i], 0, 32);
          System.arraycopy(data, spendOffset + 320 * i + 64, spendCv[i], 0, 32);
          System.arraycopy(data, spendOffset + 320 * i + 96, rk[i], 0, 32);
          System.arraycopy(data, spendOffset + 320 * i + 128, spendProof[i], 0, 192);
        }
        spendAuthSigOffset += 32;
        for (int i = 0; i < spendCount; i++) {
          System.arraycopy(data, spendAuthSigOffset + 64 * i, spendAuthSig[i], 0, 64);
        }
        //output
        receiveOffset += 32;
        for (int i = 0; i < receiveCount; i++) {
          System.arraycopy(data, receiveOffset + 288 * i, receiveCm[i], 0, 32);
          System.arraycopy(data, receiveOffset + 288 * i + 32, receiveCv[i], 0, 32);
          System.arraycopy(data, receiveOffset + 288 * i + 64, receiveEpk[i], 0, 32);
          System.arraycopy(data, receiveOffset + 288 * i + 96, receiveProof[i], 0, 192);
        }

        //copy each spendCv(receiveCv) into spendCvs(receiveCvs)
        byte[] spendCvs = new byte[spendCount * 32];
        byte[] receiveCvs = new byte[receiveCount * 32];
        for (int i = 0; i < spendCount; i++) {
          System.arraycopy(spendCv[i], 0, spendCvs, 32 * i, 32);
        }
        for (int i = 0; i < receiveCount; i++) {
          System.arraycopy(receiveCv[i], 0, receiveCvs, 32 * i, 32);
        }
        //check duplicate nullifiers
        HashSet<String> nfSet = new HashSet<>();
        for (byte[] nf : nullifier) {
          if (nfSet.contains(ByteArray.toHexString(nf))) {
            return Pair.of(true, DataWord.ZERO().getData());
          }
          nfSet.add(ByteArray.toHexString(nf));
        }
        //check duplicate output note
        HashSet<String> cmSet = new HashSet<>();
        for (byte[] cm : receiveCm) {
          if (cmSet.contains(ByteArray.toHexString(cm))) {
            return Pair.of(true, DataWord.ZERO().getData());
          }
          cmSet.add(ByteArray.toHexString(cm));
        }

        int threadCount = spendCount + receiveCount + 1;
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>(threadCount);
        ExecutorService workers;
        if (isConstantCall()) {
          workers = workersInConstantCall;
        } else {
          workers = workersInNonConstantCall;
        }

        // submit check spend task
        for (int i = 0; i < spendCount; i++) {
          Future<Boolean> futureCheckSpend = workers
              .submit(new SaplingCheckSpendTask(countDownLatch, spendCv[i], anchor[i],
                  nullifier[i], rk[i], spendProof[i], spendAuthSig[i], signHash));
          futures.add(futureCheckSpend);
        }
        //submit check output task
        for (int i = 0; i < receiveCount; i++) {
          Future<Boolean> futureCheckOutput = workers
              .submit(new SaplingCheckOutputTask(countDownLatch, receiveCv[i], receiveCm[i],
                  receiveEpk[i], receiveProof[i]));
          futures.add(futureCheckOutput);
        }
        // submit check binding signature
        Future<Boolean> futureCheckBindingSig = workers
            .submit(new SaplingCheckBingdingSig(countDownLatch, value, bindingSig,
                signHash, spendCvs, spendCount * 32, receiveCvs, receiveCount * 32));
        futures.add(futureCheckBindingSig);

        boolean withNoTimeout = countDownLatch.await(getCPUTimeLeftInNanoSecond(),
            TimeUnit.NANOSECONDS);
        boolean checkResult = true;
        for (Future<Boolean> future : futures) {
          boolean eachTaskResult = future.get();
          checkResult = checkResult && eachTaskResult;
        }
        if (checkResult) {
          return insertLeaves(frontier, leafCount, receiveCm);
        } else {
          return Pair.of(true, DataWord.ZERO().getData());
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.info("VerifyTransferProof exception: " + e.getMessage());
      } catch (Throwable any) {
        String errorMsg = any.getMessage();
        if (errorMsg == null && any.getCause() != null) {
          errorMsg = any.getCause().getMessage();
        }
        logger.info("VerifyTransferProof exception: " + errorMsg);
      }
      return Pair.of(true, DataWord.ZERO().getData());
    }

    private static class SaplingCheckSpendTask implements Callable<Boolean> {

      private byte[] cv;
      private byte[] anchor;
      private byte[] nullifier;
      private byte[] rk;
      private byte[] zkproof;
      private byte[] spendAuthSig;
      private byte[] signHash;

      private CountDownLatch countDownLatch;

      SaplingCheckSpendTask(CountDownLatch countDownLatch,
          byte[] cv, byte[] anchor, byte[] nullifier, byte[] rk,
          byte[] zkproof, byte[] spendAuthSig, byte[] signHash) {
        this.cv = cv;
        this.anchor = anchor;
        this.nullifier = nullifier;
        this.rk = rk;
        this.zkproof = zkproof;
        this.spendAuthSig = spendAuthSig;
        this.signHash = signHash;
        this.countDownLatch = countDownLatch;
      }

      @Override
      public Boolean call() throws ZksnarkException {
        boolean result;
        try {
          result = JLibrustzcash.librustzcashSaplingCheckSpendNew(
              new LibrustzcashParam.CheckSpendNewParams(this.cv, this.anchor, this.nullifier,
                  this.rk, this.zkproof, this.spendAuthSig, this.signHash));
        } catch (ZksnarkException e) {
          throw e;
        } finally {
          countDownLatch.countDown();
        }
        return result;
      }
    }

    private static class SaplingCheckOutputTask implements Callable<Boolean> {

      private byte[] cv;
      private byte[] cm;
      private byte[] ephemeralKey;
      private byte[] zkproof;

      private CountDownLatch countDownLatch;

      SaplingCheckOutputTask(CountDownLatch countDownLatch, byte[] cv, byte[] cm,
          byte[] ephemeralKey, byte[] zkproof) {
        this.cv = cv;
        this.cm = cm;
        this.ephemeralKey = ephemeralKey;
        this.zkproof = zkproof;
        this.countDownLatch = countDownLatch;
      }

      @Override
      public Boolean call() throws ZksnarkException {
        boolean result;
        try {
          result = JLibrustzcash.librustzcashSaplingCheckOutputNew(
              new LibrustzcashParam.CheckOutputNewParams(this.cv, this.cm,
                  this.ephemeralKey, this.zkproof));
        } catch (ZksnarkException e) {
          throw e;
        } finally {
          countDownLatch.countDown();
        }
        return result;
      }
    }

    private static class SaplingCheckBingdingSig implements Callable<Boolean> {

      private long valueBalance;
      private int spendCvLen;
      private int receiveCvLen;
      private byte[] bindingSig;
      private byte[] signHash;
      private byte[] spendCvs;
      private byte[] receiveCvs;

      private CountDownLatch countDownLatch;

      SaplingCheckBingdingSig(CountDownLatch countDownLatch, long valueBalance, byte[] bindingSig,
          byte[] signHash, byte[] spendCvs, int spendCvLen,
          byte[] receiveCvs, int receiveCvLen) {
        this.valueBalance = valueBalance;
        this.bindingSig = bindingSig;
        this.signHash = signHash;
        this.spendCvs = spendCvs;
        this.spendCvLen = spendCvLen;
        this.receiveCvs = receiveCvs;
        this.receiveCvLen = receiveCvLen;
        this.countDownLatch = countDownLatch;
      }

      @Override
      public Boolean call() throws ZksnarkException {
        boolean result;
        try {
          result = JLibrustzcash.librustzcashSaplingFinalCheckNew(
              new LibrustzcashParam.FinalCheckNewParams(this.valueBalance, this.bindingSig,
                  this.signHash, this.spendCvs, this.spendCvLen,
                  this.receiveCvs, this.receiveCvLen));
        } catch (ZksnarkException e) {
          throw e;
        } finally {
          countDownLatch.countDown();
        }
        return result;
      }
    }
  }

  public static class VerifyBurnProof extends VerifyProof {

    private static final int SIZE = 512;

    @Override
    public long getEnergyForData(byte[] data) {
      return 150000;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      if (data == null) {
        return Pair.of(true, DataWord.ZERO().getData());
      }
      if (data.length != SIZE) {
        return Pair.of(true, DataWord.ZERO().getData());
      }
      boolean result;
      long ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
      try {
        byte[] nullifier = new byte[32];
        byte[] anchor = new byte[32];
        byte[] cv = new byte[32];
        byte[] rk = new byte[32];
        byte[] proof = new byte[192];
        byte[] spendAuthSig = new byte[64];
        byte[] bindingSig = new byte[64];
        byte[] signHash = new byte[32];
        //spend
        System.arraycopy(data, 0, nullifier, 0, 32);
        System.arraycopy(data, 32, anchor, 0, 32);
        System.arraycopy(data, 64, cv, 0, 32);
        System.arraycopy(data, 96, rk, 0, 32);
        System.arraycopy(data, 128, proof, 0, 192);
        System.arraycopy(data, 320, spendAuthSig, 0, 64);
        long value = parseLong(data, 384);
        System.arraycopy(data, 416, bindingSig, 0, 64);
        System.arraycopy(data, 480, signHash, 0, 32);

        result = JLibrustzcash.librustzcashSaplingCheckSpend(
            new LibrustzcashParam.CheckSpendParams(
                ctx, cv, anchor, nullifier, rk, proof, spendAuthSig, signHash));
        result = result && JLibrustzcash.librustzcashSaplingFinalCheck(
            new LibrustzcashParam.FinalCheckParams(ctx, value, bindingSig, signHash));
      } catch (Throwable any) {
        result = false;
        String errorMsg = any.getMessage();
        if (errorMsg == null && any.getCause() != null) {
          errorMsg = any.getCause().getMessage();
        }
        logger.info("VerifyBurnProof exception " + errorMsg);
      } finally {
        JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
      }
      return Pair.of(true, dataBoolean(result));
    }
  }

  // compute Merkle Hash
  public static class MerkleHash extends PrecompiledContract {

    @Override
    public long getEnergyForData(byte[] data) {
      return 500;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      byte[] left = new byte[32];
      byte[] right = new byte[32];
      byte[] hash = new byte[32];
      boolean res = true;
      try {
        int level = parseInt(data);
        System.arraycopy(data, 32, left, 0, 32);
        System.arraycopy(data, 64, right, 0, 32);
        JLibrustzcash.librustzcashMerkleHash(
            new LibrustzcashParam.MerkleHashParams(level, left, right, hash));
      } catch (Throwable any) {
        res = false;
        logger.info("Compute MerkleHash failed:{}", any.getMessage());
      }
      if (res) {
        return Pair.of(true, hash);
      } else {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }
    }

    private int parseInt(byte[] data) {
      byte[] bytes = parseBytes(data, 0, 32);
      return new DataWord(bytes).intValueSafe();
    }
  }

}
