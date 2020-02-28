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

package org.tron.common.runtime.vm;

import static org.tron.common.runtime.utils.MUtil.convertToTronAddress;
import static org.tron.common.runtime.vm.DataWord.WORD_SIZE;
import static org.tron.common.utils.BIUtil.addSafely;
import static org.tron.common.utils.BIUtil.isLessThan;
import static org.tron.common.utils.BIUtil.isZero;
import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.tron.common.utils.ByteUtil.bytesToBigInteger;
import static org.tron.common.utils.ByteUtil.numberOfLeadingZeros;
import static org.tron.common.utils.ByteUtil.parseBytes;
import static org.tron.common.utils.ByteUtil.parseWord;
import static org.tron.common.utils.ByteUtil.stripLeadingZeroes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.joda.time.DateTime;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.zksnark.BN128;
import org.tron.common.crypto.zksnark.BN128Fp;
import org.tron.common.crypto.zksnark.BN128G1;
import org.tron.common.crypto.zksnark.BN128G2;
import org.tron.common.crypto.zksnark.Fp;
import org.tron.common.crypto.zksnark.PairingCheck;
import org.tron.common.runtime.config.VMConfig;
import org.tron.common.runtime.utils.MUtil;
import org.tron.common.runtime.vm.program.Program;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.storage.Deposit;
import org.tron.common.utils.BIUtil;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ZksnarkException;
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
  private static final ValidateProof validateProof = new ValidateProof();
  private static final CalHash calHash = new CalHash();
  private static final CalTimeContract calTimeContract = new CalTimeContract();

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
  private static final DataWord validateProofAddr = new DataWord(
          "000000000000000000000000000000000000000000000000000000000000000F");
  private static final DataWord calHashAddr = new DataWord(
          "0000000000000000000000000000000000000000000000000000000000000010");
  private static final DataWord calTimeAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000011");

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
    if (address.equals(validateProofAddr)) {
      return validateProof;
    }
    if (address.equals(calHashAddr)) {
      return calHash;
    }
    if (address.equals(calTimeAddr)) {
      return calTimeContract;
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

  public static abstract class PrecompiledContract {

    protected static final byte[] DATA_FALSE = new byte[WORD_SIZE];


    public abstract long getEnergyForData(byte[] data);

    public abstract Pair<Boolean, byte[]> execute(byte[] data);

    private byte[] callerAddress;

    public void setCallerAddress(byte[] callerAddress) {
      this.callerAddress = callerAddress.clone();
    }

    public void setDeposit(Deposit deposit) {
      this.deposit = deposit;
    }

    public void setResult(ProgramResult result) {
      this.result = result;
    }

    private Deposit deposit;

    private ProgramResult result;

    public byte[] getCallerAddress() {
      return callerAddress.clone();
    }

    public Deposit getDeposit() {
      return deposit;
    }

    public ProgramResult getResult() {
      return result;
    }

    @Setter
    @Getter
    private boolean isConstantCall;

    @Getter
    @Setter
    private long vmShouldEndInUs;


    protected long getCPUTimeLeftInNanoSecond() {
      long left = getVmShouldEndInUs() * Constant.ONE_THOUSAND - System.nanoTime();
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
        return Pair.of(true, Sha256Hash.hash(EMPTY_BYTE_ARRAY));
      }
      return Pair.of(true, Sha256Hash.hash(data));
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
      byte[] orig = Sha256Hash.hash(data);
      System.arraycopy(orig, 0, target, 0, 20);
      return Pair.of(true, Sha256Hash.hash(target));
    }
  }


  public static class ECRecover extends PrecompiledContract {

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

        ECKey.ECDSASignature signature = ECKey.ECDSASignature.fromComponents(r, s, v[31]);
        if (validateV(v) && signature.validateComponents()) {
          out = new DataWord(ECKey.signatureToAddress(h, signature));
        }
      } catch (Throwable any) {
      }

      if (out == null) {
        return Pair.of(true, EMPTY_BYTE_ARRAY);
      } else {
        return Pair.of(true, out.getData());
      }
    }

    private static boolean validateV(byte[] v) {
      for (int i = 0; i < v.length - 1; i++) {
        if (v[i] != 0) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Computes modular exponentiation on big numbers
   *
   * format of data[] array: [length_of_BASE] [length_of_EXPONENT] [length_of_MODULUS] [BASE]
   * [EXPONENT] [MODULUS] where every length is a 32-byte left-padded integer representing the
   * number of bytes. Call data is assumed to be infinitely right-padded with zero bytes.
   *
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
   *
   * input data[]:<br/> two points encoded as (x, y), where x and y are 32-byte left-padded
   * integers,<br/> if input is shorter than expected, it's assumed to be right-padded with zero
   * bytes<br/> <br/>
   *
   * output:<br/> resulting point (x', y'), where x and y encoded as 32-byte left-padded
   * integers<br/>
   */
  public static class BN128Addition extends PrecompiledContract {

    @Override
    public long getEnergyForData(byte[] data) {
      return 500;
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
   *
   * input data[]:<br/> point encoded as (x, y) is followed by scalar s, where x, y and s are
   * 32-byte left-padded integers,<br/> if input is shorter than expected, it's assumed to be
   * right-padded with zero bytes<br/> <br/>
   *
   * output:<br/> resulting point (x', y'), where x and y encoded as 32-byte left-padded
   * integers<br/>
   */
  public static class BN128Multiplication extends PrecompiledContract {

    @Override
    public long getEnergyForData(byte[] data) {
      return 40000;
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
   *
   * Input data[]: <br/> an array of points (a1, b1, ... , ak, bk), <br/> where "ai" is a point of
   * {@link BN128Fp} curve and encoded as two 32-byte left-padded integers (x; y) <br/> "bi" is a
   * point of {@link BN128G2} curve and encoded as four 32-byte left-padded integers {@code (ai + b;
   * ci + d)}, each coordinate of the point is a big-endian {@link } number, so {@code b} precedes
   * {@code a} in the encoding: {@code (b, a; d, c)} <br/> thus each pair (ai, bi) has 192 bytes
   * length, if 192 is not a multiple of {@code data.length} then execution fails <br/> the number
   * of pairs is derived from input length by dividing it by 192 (the length of a pair) <br/> <br/>
   *
   * output: <br/> pairing product which is either 0 or 1, encoded as 32-byte left-padded integer
   * <br/>
   */
  public static class BN128Pairing extends PrecompiledContract {

    private static final int PAIR_SIZE = 192;

    @Override
    public long getEnergyForData(byte[] data) {

      if (data == null) {
        return 100000;
      }

      return 80000L * (data.length / PAIR_SIZE) + 100000;
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
          .merge(MUtil.convertToTronAddress(addr), ByteArray.fromInt(permissionId), data);
      byte[] hash = Sha256Hash.hash(combine);

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
        //for static call not use thread pool to avoid potential effect
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
      ECKey.ECDSASignature signature = ECKey.ECDSASignature.fromComponents(r, s, v);
      if (signature.validateComponents()) {
        out = ECKey.signatureToAddress(hash, signature);
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

  public static class ValidateProof extends PrecompiledContract {

    private static final int MINT_SIZE = 416 + 32 * 33 + 32;
    private static final int TRANSFER_SIZE = 1056 + 32 * 33 + 32;
    private static final int BURN_SIZE = 512;
    private static final long TREE_WIDTH = 1L << 32;

    private static final byte[][] UNCOMMITTED = {
            {(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00},
            {(byte) 0x81, (byte) 0x7d, (byte) 0xe3, (byte) 0x6a, (byte) 0xb2, (byte) 0xd5, (byte) 0x7f, (byte) 0xeb, (byte) 0x07, (byte) 0x76, (byte) 0x34, (byte) 0xbc, (byte) 0xa7, (byte) 0x78, (byte) 0x19, (byte) 0xc8, (byte) 0xe0, (byte) 0xbd, (byte) 0x29, (byte) 0x8c, (byte) 0x04, (byte) 0xf6, (byte) 0xfe, (byte) 0xd0, (byte) 0xe6, (byte) 0xa8, (byte) 0x3c, (byte) 0xc1, (byte) 0x35, (byte) 0x6c, (byte) 0xa1, (byte) 0x55},
            {(byte) 0xff, (byte) 0xe9, (byte) 0xfc, (byte) 0x03, (byte) 0xf1, (byte) 0x8b, (byte) 0x17, (byte) 0x6c, (byte) 0x99, (byte) 0x88, (byte) 0x06, (byte) 0x43, (byte) 0x9f, (byte) 0xf0, (byte) 0xbb, (byte) 0x8a, (byte) 0xd1, (byte) 0x93, (byte) 0xaf, (byte) 0xdb, (byte) 0x27, (byte) 0xb2, (byte) 0xcc, (byte) 0xbc, (byte) 0x88, (byte) 0x85, (byte) 0x69, (byte) 0x16, (byte) 0xdd, (byte) 0x80, (byte) 0x4e, (byte) 0x34},
            {(byte) 0xd8, (byte) 0x28, (byte) 0x33, (byte) 0x86, (byte) 0xef, (byte) 0x2e, (byte) 0xf0, (byte) 0x7e, (byte) 0xbd, (byte) 0xbb, (byte) 0x43, (byte) 0x83, (byte) 0xc1, (byte) 0x2a, (byte) 0x73, (byte) 0x9a, (byte) 0x95, (byte) 0x3a, (byte) 0x4d, (byte) 0x6e, (byte) 0x0d, (byte) 0x6f, (byte) 0xb1, (byte) 0x13, (byte) 0x9a, (byte) 0x40, (byte) 0x36, (byte) 0xd6, (byte) 0x93, (byte) 0xbf, (byte) 0xbb, (byte) 0x6c},
            {(byte) 0xe1, (byte) 0x10, (byte) 0xde, (byte) 0x65, (byte) 0xc9, (byte) 0x07, (byte) 0xb9, (byte) 0xde, (byte) 0xa4, (byte) 0xae, (byte) 0x0b, (byte) 0xd8, (byte) 0x3a, (byte) 0x4b, (byte) 0x0a, (byte) 0x51, (byte) 0xbe, (byte) 0xa1, (byte) 0x75, (byte) 0x64, (byte) 0x6a, (byte) 0x64, (byte) 0xc1, (byte) 0x2b, (byte) 0x4c, (byte) 0x9f, (byte) 0x93, (byte) 0x1b, (byte) 0x2c, (byte) 0xb3, (byte) 0x1b, (byte) 0x49},
            {(byte) 0x91, (byte) 0x2d, (byte) 0x82, (byte) 0xb2, (byte) 0xc2, (byte) 0xbc, (byte) 0xa2, (byte) 0x31, (byte) 0xf7, (byte) 0x1e, (byte) 0xfc, (byte) 0xf6, (byte) 0x17, (byte) 0x37, (byte) 0xfb, (byte) 0xf0, (byte) 0xa0, (byte) 0x8b, (byte) 0xef, (byte) 0xa0, (byte) 0x41, (byte) 0x62, (byte) 0x15, (byte) 0xae, (byte) 0xef, (byte) 0x53, (byte) 0xe8, (byte) 0xbb, (byte) 0x6d, (byte) 0x23, (byte) 0x39, (byte) 0x0a},
            {(byte) 0x8a, (byte) 0xc9, (byte) 0xcf, (byte) 0x9c, (byte) 0x39, (byte) 0x1e, (byte) 0x3f, (byte) 0xd4, (byte) 0x28, (byte) 0x91, (byte) 0xd2, (byte) 0x72, (byte) 0x38, (byte) 0xa8, (byte) 0x1a, (byte) 0x8a, (byte) 0x5c, (byte) 0x1d, (byte) 0x3a, (byte) 0x72, (byte) 0xb1, (byte) 0xbc, (byte) 0xbe, (byte) 0xa8, (byte) 0xcf, (byte) 0x44, (byte) 0xa5, (byte) 0x8c, (byte) 0xe7, (byte) 0x38, (byte) 0x96, (byte) 0x13},
            {(byte) 0xd6, (byte) 0xc6, (byte) 0x39, (byte) 0xac, (byte) 0x24, (byte) 0xb4, (byte) 0x6b, (byte) 0xd1, (byte) 0x93, (byte) 0x41, (byte) 0xc9, (byte) 0x1b, (byte) 0x13, (byte) 0xfd, (byte) 0xca, (byte) 0xb3, (byte) 0x15, (byte) 0x81, (byte) 0xdd, (byte) 0xaf, (byte) 0x7f, (byte) 0x14, (byte) 0x11, (byte) 0x33, (byte) 0x6a, (byte) 0x27, (byte) 0x1f, (byte) 0x3d, (byte) 0x0a, (byte) 0xa5, (byte) 0x28, (byte) 0x13},
            {(byte) 0x7b, (byte) 0x99, (byte) 0xab, (byte) 0xdc, (byte) 0x37, (byte) 0x30, (byte) 0x99, (byte) 0x1c, (byte) 0xc9, (byte) 0x27, (byte) 0x47, (byte) 0x27, (byte) 0xd7, (byte) 0xd8, (byte) 0x2d, (byte) 0x28, (byte) 0xcb, (byte) 0x79, (byte) 0x4e, (byte) 0xdb, (byte) 0xc7, (byte) 0x03, (byte) 0x4b, (byte) 0x4f, (byte) 0x00, (byte) 0x53, (byte) 0xff, (byte) 0x7c, (byte) 0x4b, (byte) 0x68, (byte) 0x04, (byte) 0x44},
            {(byte) 0x43, (byte) 0xff, (byte) 0x54, (byte) 0x57, (byte) 0xf1, (byte) 0x3b, (byte) 0x92, (byte) 0x6b, (byte) 0x61, (byte) 0xdf, (byte) 0x55, (byte) 0x2d, (byte) 0x4e, (byte) 0x40, (byte) 0x2e, (byte) 0xe6, (byte) 0xdc, (byte) 0x14, (byte) 0x63, (byte) 0xf9, (byte) 0x9a, (byte) 0x53, (byte) 0x5f, (byte) 0x9a, (byte) 0x71, (byte) 0x34, (byte) 0x39, (byte) 0x26, (byte) 0x4d, (byte) 0x5b, (byte) 0x61, (byte) 0x6b},
            {(byte) 0xba, (byte) 0x49, (byte) 0xb6, (byte) 0x59, (byte) 0xfb, (byte) 0xd0, (byte) 0xb7, (byte) 0x33, (byte) 0x42, (byte) 0x11, (byte) 0xea, (byte) 0x6a, (byte) 0x9d, (byte) 0x9d, (byte) 0xf1, (byte) 0x85, (byte) 0xc7, (byte) 0x57, (byte) 0xe7, (byte) 0x0a, (byte) 0xa8, (byte) 0x1d, (byte) 0xa5, (byte) 0x62, (byte) 0xfb, (byte) 0x91, (byte) 0x2b, (byte) 0x84, (byte) 0xf4, (byte) 0x9b, (byte) 0xce, (byte) 0x72},
            {(byte) 0x47, (byte) 0x77, (byte) 0xc8, (byte) 0x77, (byte) 0x6a, (byte) 0x3b, (byte) 0x1e, (byte) 0x69, (byte) 0xb7, (byte) 0x3a, (byte) 0x62, (byte) 0xfa, (byte) 0x70, (byte) 0x1f, (byte) 0xa4, (byte) 0xf7, (byte) 0xa6, (byte) 0x28, (byte) 0x2d, (byte) 0x9a, (byte) 0xee, (byte) 0x2c, (byte) 0x7a, (byte) 0x6b, (byte) 0x82, (byte) 0xe7, (byte) 0x93, (byte) 0x7d, (byte) 0x70, (byte) 0x81, (byte) 0xc2, (byte) 0x3c},
            {(byte) 0xec, (byte) 0x67, (byte) 0x71, (byte) 0x14, (byte) 0xc2, (byte) 0x72, (byte) 0x06, (byte) 0xf5, (byte) 0xde, (byte) 0xbc, (byte) 0x1c, (byte) 0x1e, (byte) 0xd6, (byte) 0x6f, (byte) 0x95, (byte) 0xe2, (byte) 0xb1, (byte) 0x88, (byte) 0x5d, (byte) 0xa5, (byte) 0xb7, (byte) 0xbe, (byte) 0x3d, (byte) 0x73, (byte) 0x6b, (byte) 0x1d, (byte) 0xe9, (byte) 0x85, (byte) 0x79, (byte) 0x47, (byte) 0x30, (byte) 0x48},
            {(byte) 0x1b, (byte) 0x77, (byte) 0xda, (byte) 0xc4, (byte) 0xd2, (byte) 0x4f, (byte) 0xb7, (byte) 0x25, (byte) 0x8c, (byte) 0x3c, (byte) 0x52, (byte) 0x87, (byte) 0x04, (byte) 0xc5, (byte) 0x94, (byte) 0x30, (byte) 0xb6, (byte) 0x30, (byte) 0x71, (byte) 0x8b, (byte) 0xec, (byte) 0x48, (byte) 0x64, (byte) 0x21, (byte) 0x83, (byte) 0x70, (byte) 0x21, (byte) 0xcf, (byte) 0x75, (byte) 0xda, (byte) 0xb6, (byte) 0x51},
            {(byte) 0xbd, (byte) 0x74, (byte) 0xb2, (byte) 0x5a, (byte) 0xac, (byte) 0xb9, (byte) 0x23, (byte) 0x78, (byte) 0xa8, (byte) 0x71, (byte) 0xbf, (byte) 0x27, (byte) 0xd2, (byte) 0x25, (byte) 0xcf, (byte) 0xc2, (byte) 0x6b, (byte) 0xac, (byte) 0xa3, (byte) 0x44, (byte) 0xa1, (byte) 0xea, (byte) 0x35, (byte) 0xfd, (byte) 0xd9, (byte) 0x45, (byte) 0x10, (byte) 0xf3, (byte) 0xd1, (byte) 0x57, (byte) 0x08, (byte) 0x2c},
            {(byte) 0xd6, (byte) 0xac, (byte) 0xde, (byte) 0xdf, (byte) 0x95, (byte) 0xf6, (byte) 0x08, (byte) 0xe0, (byte) 0x9f, (byte) 0xa5, (byte) 0x3f, (byte) 0xb4, (byte) 0x3d, (byte) 0xcd, (byte) 0x09, (byte) 0x90, (byte) 0x47, (byte) 0x57, (byte) 0x26, (byte) 0xc5, (byte) 0x13, (byte) 0x12, (byte) 0x10, (byte) 0xc9, (byte) 0xe5, (byte) 0xca, (byte) 0xea, (byte) 0xb9, (byte) 0x7f, (byte) 0x0e, (byte) 0x64, (byte) 0x2f},
            {(byte) 0x1e, (byte) 0xa6, (byte) 0x67, (byte) 0x5f, (byte) 0x95, (byte) 0x51, (byte) 0xee, (byte) 0xb9, (byte) 0xdf, (byte) 0xaa, (byte) 0xa9, (byte) 0x24, (byte) 0x7b, (byte) 0xc9, (byte) 0x85, (byte) 0x82, (byte) 0x70, (byte) 0xd3, (byte) 0xd3, (byte) 0xa4, (byte) 0xc5, (byte) 0xaf, (byte) 0xa7, (byte) 0x17, (byte) 0x7a, (byte) 0x98, (byte) 0x4d, (byte) 0x5e, (byte) 0xd1, (byte) 0xbe, (byte) 0x24, (byte) 0x51},
            {(byte) 0x6e, (byte) 0xdb, (byte) 0x16, (byte) 0xd0, (byte) 0x19, (byte) 0x07, (byte) 0xb7, (byte) 0x59, (byte) 0x97, (byte) 0x7d, (byte) 0x76, (byte) 0x50, (byte) 0xda, (byte) 0xd7, (byte) 0xe3, (byte) 0xec, (byte) 0x04, (byte) 0x9a, (byte) 0xf1, (byte) 0xa3, (byte) 0xd8, (byte) 0x75, (byte) 0x38, (byte) 0x0b, (byte) 0x69, (byte) 0x7c, (byte) 0x86, (byte) 0x2c, (byte) 0x9e, (byte) 0xc5, (byte) 0xd5, (byte) 0x1c},
            {(byte) 0xcd, (byte) 0x1c, (byte) 0x8d, (byte) 0xbf, (byte) 0x6e, (byte) 0x3a, (byte) 0xcc, (byte) 0x7a, (byte) 0x80, (byte) 0x43, (byte) 0x9b, (byte) 0xc4, (byte) 0x96, (byte) 0x2c, (byte) 0xf2, (byte) 0x5b, (byte) 0x9d, (byte) 0xce, (byte) 0x7c, (byte) 0x89, (byte) 0x6f, (byte) 0x3a, (byte) 0x5b, (byte) 0xd7, (byte) 0x08, (byte) 0x03, (byte) 0xfc, (byte) 0x5a, (byte) 0x0e, (byte) 0x33, (byte) 0xcf, (byte) 0x00},
            {(byte) 0x6a, (byte) 0xca, (byte) 0x84, (byte) 0x48, (byte) 0xd8, (byte) 0x26, (byte) 0x3e, (byte) 0x54, (byte) 0x7d, (byte) 0x5f, (byte) 0xf2, (byte) 0x95, (byte) 0x0e, (byte) 0x2e, (byte) 0xd3, (byte) 0x83, (byte) 0x9e, (byte) 0x99, (byte) 0x8d, (byte) 0x31, (byte) 0xcb, (byte) 0xc6, (byte) 0xac, (byte) 0x9f, (byte) 0xd5, (byte) 0x7b, (byte) 0xc6, (byte) 0x00, (byte) 0x2b, (byte) 0x15, (byte) 0x92, (byte) 0x16},
            {(byte) 0x8d, (byte) 0x5f, (byte) 0xa4, (byte) 0x3e, (byte) 0x5a, (byte) 0x10, (byte) 0xd1, (byte) 0x16, (byte) 0x05, (byte) 0xac, (byte) 0x74, (byte) 0x30, (byte) 0xba, (byte) 0x1f, (byte) 0x5d, (byte) 0x81, (byte) 0xfb, (byte) 0x1b, (byte) 0x68, (byte) 0xd2, (byte) 0x9a, (byte) 0x64, (byte) 0x04, (byte) 0x05, (byte) 0x76, (byte) 0x77, (byte) 0x49, (byte) 0xe8, (byte) 0x41, (byte) 0x52, (byte) 0x76, (byte) 0x73},
            {(byte) 0x08, (byte) 0xee, (byte) 0xab, (byte) 0x0c, (byte) 0x13, (byte) 0xab, (byte) 0xd6, (byte) 0x06, (byte) 0x9e, (byte) 0x63, (byte) 0x10, (byte) 0x19, (byte) 0x7b, (byte) 0xf8, (byte) 0x0f, (byte) 0x9c, (byte) 0x1e, (byte) 0xa6, (byte) 0xde, (byte) 0x78, (byte) 0xfd, (byte) 0x19, (byte) 0xcb, (byte) 0xae, (byte) 0x24, (byte) 0xd4, (byte) 0xa5, (byte) 0x20, (byte) 0xe6, (byte) 0xcf, (byte) 0x30, (byte) 0x23},
            {(byte) 0x07, (byte) 0x69, (byte) 0x55, (byte) 0x7b, (byte) 0xc6, (byte) 0x82, (byte) 0xb1, (byte) 0xbf, (byte) 0x30, (byte) 0x86, (byte) 0x46, (byte) 0xfd, (byte) 0x0b, (byte) 0x22, (byte) 0xe6, (byte) 0x48, (byte) 0xe8, (byte) 0xb9, (byte) 0xe9, (byte) 0x8f, (byte) 0x57, (byte) 0xe2, (byte) 0x9f, (byte) 0x5a, (byte) 0xf4, (byte) 0x0f, (byte) 0x6e, (byte) 0xdb, (byte) 0x83, (byte) 0x3e, (byte) 0x2c, (byte) 0x49},
            {(byte) 0x4c, (byte) 0x69, (byte) 0x37, (byte) 0xd7, (byte) 0x8f, (byte) 0x42, (byte) 0x68, (byte) 0x5f, (byte) 0x84, (byte) 0xb4, (byte) 0x3a, (byte) 0xd3, (byte) 0xb7, (byte) 0xb0, (byte) 0x0f, (byte) 0x81, (byte) 0x28, (byte) 0x56, (byte) 0x62, (byte) 0xf8, (byte) 0x5c, (byte) 0x6a, (byte) 0x68, (byte) 0xef, (byte) 0x11, (byte) 0xd6, (byte) 0x2a, (byte) 0xd1, (byte) 0xa3, (byte) 0xee, (byte) 0x08, (byte) 0x50},
            {(byte) 0xfe, (byte) 0xe0, (byte) 0xe5, (byte) 0x28, (byte) 0x02, (byte) 0xcb, (byte) 0x0c, (byte) 0x46, (byte) 0xb1, (byte) 0xeb, (byte) 0x4d, (byte) 0x37, (byte) 0x6c, (byte) 0x62, (byte) 0x69, (byte) 0x7f, (byte) 0x47, (byte) 0x59, (byte) 0xf6, (byte) 0xc8, (byte) 0x91, (byte) 0x7f, (byte) 0xa3, (byte) 0x52, (byte) 0x57, (byte) 0x12, (byte) 0x02, (byte) 0xfd, (byte) 0x77, (byte) 0x8f, (byte) 0xd7, (byte) 0x12},
            {(byte) 0x16, (byte) 0xd6, (byte) 0x25, (byte) 0x29, (byte) 0x68, (byte) 0x97, (byte) 0x1a, (byte) 0x83, (byte) 0xda, (byte) 0x85, (byte) 0x21, (byte) 0xd6, (byte) 0x53, (byte) 0x82, (byte) 0xe6, (byte) 0x1f, (byte) 0x01, (byte) 0x76, (byte) 0x64, (byte) 0x6d, (byte) 0x77, (byte) 0x1c, (byte) 0x91, (byte) 0x52, (byte) 0x8e, (byte) 0x32, (byte) 0x76, (byte) 0xee, (byte) 0x45, (byte) 0x38, (byte) 0x3e, (byte) 0x4a},
            {(byte) 0xd2, (byte) 0xe1, (byte) 0x64, (byte) 0x2c, (byte) 0x9a, (byte) 0x46, (byte) 0x22, (byte) 0x29, (byte) 0x28, (byte) 0x9e, (byte) 0x5b, (byte) 0x0e, (byte) 0x3b, (byte) 0x7f, (byte) 0x90, (byte) 0x08, (byte) 0xe0, (byte) 0x30, (byte) 0x1c, (byte) 0xbb, (byte) 0x93, (byte) 0x38, (byte) 0x5e, (byte) 0xe0, (byte) 0xe2, (byte) 0x1d, (byte) 0xa2, (byte) 0x54, (byte) 0x50, (byte) 0x73, (byte) 0xcb, (byte) 0x58},
            {(byte) 0xa5, (byte) 0x12, (byte) 0x2c, (byte) 0x08, (byte) 0xff, (byte) 0x9c, (byte) 0x16, (byte) 0x1d, (byte) 0x9c, (byte) 0xa6, (byte) 0xfc, (byte) 0x46, (byte) 0x20, (byte) 0x73, (byte) 0x39, (byte) 0x6c, (byte) 0x7d, (byte) 0x7d, (byte) 0x38, (byte) 0xe8, (byte) 0xee, (byte) 0x48, (byte) 0xcd, (byte) 0xb3, (byte) 0xbe, (byte) 0xa7, (byte) 0xe2, (byte) 0x23, (byte) 0x01, (byte) 0x34, (byte) 0xed, (byte) 0x6a},
            {(byte) 0x28, (byte) 0xe7, (byte) 0xb8, (byte) 0x41, (byte) 0xdc, (byte) 0xbc, (byte) 0x47, (byte) 0xcc, (byte) 0xeb, (byte) 0x69, (byte) 0xd7, (byte) 0xcb, (byte) 0x8d, (byte) 0x94, (byte) 0x24, (byte) 0x5f, (byte) 0xb7, (byte) 0xcb, (byte) 0x2b, (byte) 0xa3, (byte) 0xa7, (byte) 0xa6, (byte) 0xbc, (byte) 0x18, (byte) 0xf1, (byte) 0x3f, (byte) 0x94, (byte) 0x5f, (byte) 0x7d, (byte) 0xbd, (byte) 0x6e, (byte) 0x2a},
            {(byte) 0xe1, (byte) 0xf3, (byte) 0x4b, (byte) 0x03, (byte) 0x4d, (byte) 0x4a, (byte) 0x3c, (byte) 0xd2, (byte) 0x85, (byte) 0x57, (byte) 0xe2, (byte) 0x90, (byte) 0x7e, (byte) 0xbf, (byte) 0x99, (byte) 0x0c, (byte) 0x91, (byte) 0x8f, (byte) 0x64, (byte) 0xec, (byte) 0xb5, (byte) 0x0a, (byte) 0x94, (byte) 0xf0, (byte) 0x1d, (byte) 0x6f, (byte) 0xda, (byte) 0x5c, (byte) 0xa5, (byte) 0xc7, (byte) 0xef, (byte) 0x72},
            {(byte) 0x12, (byte) 0x93, (byte) 0x5f, (byte) 0x14, (byte) 0xb6, (byte) 0x76, (byte) 0x50, (byte) 0x9b, (byte) 0x81, (byte) 0xeb, (byte) 0x49, (byte) 0xef, (byte) 0x25, (byte) 0xf3, (byte) 0x92, (byte) 0x69, (byte) 0xed, (byte) 0x72, (byte) 0x30, (byte) 0x92, (byte) 0x38, (byte) 0xb4, (byte) 0xc1, (byte) 0x45, (byte) 0x80, (byte) 0x35, (byte) 0x44, (byte) 0xb6, (byte) 0x46, (byte) 0xdc, (byte) 0xa6, (byte) 0x2d},
            {(byte) 0xb2, (byte) 0xee, (byte) 0xd0, (byte) 0x31, (byte) 0xd4, (byte) 0xd6, (byte) 0xa4, (byte) 0xf0, (byte) 0x2a, (byte) 0x09, (byte) 0x7f, (byte) 0x80, (byte) 0xb5, (byte) 0x4c, (byte) 0xc1, (byte) 0x54, (byte) 0x1d, (byte) 0x41, (byte) 0x63, (byte) 0xc6, (byte) 0xb6, (byte) 0xf5, (byte) 0x97, (byte) 0x1f, (byte) 0x88, (byte) 0xb6, (byte) 0xe4, (byte) 0x1d, (byte) 0x35, (byte) 0xc5, (byte) 0x38, (byte) 0x14},
            {(byte) 0xfb, (byte) 0xc2, (byte) 0xf4, (byte) 0x30, (byte) 0x0c, (byte) 0x01, (byte) 0xf0, (byte) 0xb7, (byte) 0x82, (byte) 0x0d, (byte) 0x00, (byte) 0xe3, (byte) 0x34, (byte) 0x7c, (byte) 0x8d, (byte) 0xa4, (byte) 0xee, (byte) 0x61, (byte) 0x46, (byte) 0x74, (byte) 0x37, (byte) 0x6c, (byte) 0xbc, (byte) 0x45, (byte) 0x35, (byte) 0x9d, (byte) 0xaa, (byte) 0x54, (byte) 0xf9, (byte) 0xb5, (byte) 0x49, (byte) 0x3e}
    };


    @Override
    public long getEnergyForData(byte[] data) {
      return 0;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      if (data == null) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }
      if (data.length == MINT_SIZE) {
        return checkMint(data);
      } else if (data.length == TRANSFER_SIZE) {
        return checkTransfer(data);
      } else if (data.length == BURN_SIZE) {
        return checkBurn(data);
      } else {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }
    }

    //TODO: optimize read frontier
    //mint: 1 transparent --> 1 shielded
    private Pair<Boolean, byte[]> checkMint(byte[] data) {
      long start_time = System.currentTimeMillis();

      byte[] cv = new byte[32];
      byte[] cm = new byte[32];
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
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }
      boolean result;

      //verify receiveProof && bindingSignature
      long ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
      try {
        result = JLibrustzcash.librustzcashSaplingCheckOutput(
                new LibrustzcashParam.CheckOutputParams(ctx, cv, cm, epk, proof));

        long valueBalance = -value;

        result &= JLibrustzcash.librustzcashSaplingFinalCheck(
                new LibrustzcashParam.FinalCheckParams(ctx, valueBalance, bindingSig, signHash));
      } catch (Throwable any) {
        result = false;
      } finally {
        JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
      }
      if (!result) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      long costTime = System.currentTimeMillis() - start_time;
      logger.info("Mint verify successfully, " + "check cost is: " + costTime + "ms");

      long startTimeInsert = System.currentTimeMillis();
      Pair<Boolean, byte[]> pair = insertLeaf(frontier, cm, leafCount);
      costTime = System.currentTimeMillis() - startTimeInsert;
      logger.info("insertLeaf cost is: " + costTime + "ms");

      costTime = System.currentTimeMillis() - start_time;
      logger.info("total cost is: " + costTime + "ms");

      return pair;

    }

    private static class SaplingCheckSpendTask implements Callable<Boolean> {

      private long ctx;
      private byte[] cv;
      private byte[] anchor;
      private byte[] nullifier;
      private byte[] rk;
      private byte[] zkproof;
      private byte[] spendAuthSig;
      private byte[] sighashValue;

      private CountDownLatch countDownLatch;

      SaplingCheckSpendTask(CountDownLatch countDownLatch, long ctx, byte[] cv, byte[] anchor,
          byte[] nullifier, byte[] rk, byte[] zkproof, byte[] spendAuthSig, byte[] sighashValue) {
        this.ctx = ctx;
        this.cv = cv;
        this.anchor = anchor;
        this.nullifier = nullifier;
        this.rk = rk;
        this.zkproof = zkproof;
        this.spendAuthSig = spendAuthSig;
        this.sighashValue = sighashValue;

        this.countDownLatch = countDownLatch;
      }

      @Override
      public Boolean call() throws ZksnarkException {
        boolean result;
        try {
          long verifyStartTime = System.currentTimeMillis();
          result = JLibrustzcash.librustzcashSaplingCheckSpend(
              new LibrustzcashParam.CheckSpendParams(this.ctx, this.cv, this.anchor, this.nullifier,
                  this.rk, this.zkproof, this.spendAuthSig, this.sighashValue));
          long checkSpendEndTime = System.currentTimeMillis();
          logger.info("parallel Transfer checkSpend cost is: " + (checkSpendEndTime - verifyStartTime) + "ms" +
              ", result is " + result);

        } catch (ZksnarkException e) {
          throw e;
        } finally {
          countDownLatch.countDown();
        }
        return result;
      }
    }

    private static class SaplingCheckOutput implements Callable<Boolean> {

      private long ctx;
      private byte[] cv;
      private byte[] cm;
      private byte[] ephemeralKey;
      private byte[] zkproof;

      private CountDownLatch countDownLatch;

      SaplingCheckOutput(CountDownLatch countDownLatch, long ctx, byte[] cv, byte[] cm, byte[] ephemeralKey,
          byte[] zkproof) {
        this.ctx = ctx;
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
          long verifyStartTime = System.currentTimeMillis();
          result = JLibrustzcash.librustzcashSaplingCheckOutput(
              new LibrustzcashParam.CheckOutputParams(this.ctx, this.cv, this.cm, this.ephemeralKey, this.zkproof));
          long checkOutputEndTime = System.currentTimeMillis();
          logger.info("parallel Transfer checkOutput cost is: " + (checkOutputEndTime - verifyStartTime) + "ms" +
              ", result is " + result);

        } catch (ZksnarkException e) {
          throw e;
        } finally {
          countDownLatch.countDown();
        }
        return result;
      }
    }

      //transfer: 1 shielded --> 2 shielded
    private Pair<Boolean, byte[]> checkTransfer(byte[] data) {
      long startTime = System.currentTimeMillis();

      byte[] spendCv = new byte[32];
      byte[] anchor = new byte[32];
      byte[] nullifier = new byte[32];
      byte[] rk = new byte[32];
      byte[] spendAuthSig = new byte[64];
      byte[] spendProof = new byte[192];

      byte[][] cm = new byte[2][32];
      byte[] receiveCv0 = new byte[32];
      byte[] receiveEpk0 = new byte[32];
      byte[] receiveProof0 = new byte[192];

      byte[] receiveCv1 = new byte[32];
      byte[] receiveEpk1 = new byte[32];
      byte[] receiveProof1 = new byte[192];

      byte[] bindingSig = new byte[64];
      byte[] signHash = new byte[32];

      byte[][] frontier = new byte[33][32];

      //spend
      System.arraycopy(data, 0, spendCv, 0, 32);
      System.arraycopy(data, 32, rk, 0, 32);
      System.arraycopy(data, 64, spendAuthSig, 0, 64);
      System.arraycopy(data, 128, spendProof, 0, 192);
      System.arraycopy(data, 320, anchor, 0, 32);
      System.arraycopy(data, 352, nullifier, 0, 32);
      //receive0
      System.arraycopy(data, 384, receiveCv0, 0, 32);
      System.arraycopy(data, 416, cm[0], 0, 32);
      System.arraycopy(data, 448, receiveEpk0, 0, 32);
      System.arraycopy(data, 480, receiveProof0, 0, 192);
      //receive1
      System.arraycopy(data, 672, receiveCv1, 0, 32);
      System.arraycopy(data, 704, cm[1], 0, 32);
      System.arraycopy(data, 736, receiveEpk1, 0, 32);
      System.arraycopy(data, 768, receiveProof1, 0, 192);

      System.arraycopy(data, 960, bindingSig, 0, 64);
      System.arraycopy(data, 1024, signHash, 0, 32);

      for (int i = 0; i < 33; i++) {
        System.arraycopy(data, i * 32 + 1056, frontier[i], 0, 32);
      }
      long leafCount = parseLong(data, 2112);
      if (leafCount >= TREE_WIDTH - 1) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      boolean parallel = true;
      boolean result;

      //verify spendProof, receiveProof && bindingSignature
      long verifyStartTime = System.currentTimeMillis();
      long ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();

      if (parallel) {
        // thread poll
        int threadCount = 3;
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>(threadCount);
        ExecutorService validateSignService = Executors.newFixedThreadPool(3);

        // submit 3 check task
        Future<Boolean> future1 = validateSignService
            .submit(new SaplingCheckSpendTask(countDownLatch, ctx, spendCv, anchor, nullifier, rk, spendProof, spendAuthSig, signHash));
        futures.add(future1);
        Future<Boolean> future2 = validateSignService
            .submit(new SaplingCheckOutput(countDownLatch, ctx, receiveCv0, cm[0], receiveEpk0, receiveProof0));
        futures.add(future2);
        Future<Boolean> future3 = validateSignService
            .submit(new SaplingCheckOutput(countDownLatch, ctx, receiveCv1, cm[1], receiveEpk1, receiveProof1));
        futures.add(future3);

        result = true;
        try {
          countDownLatch.await();
          for (Future<Boolean> future : futures) {
            boolean fResult = future.get();
            // logger.info("future.get() is " + fResult);
            result &= fResult;
          }

          long checkFinalCheckStartTime = System.currentTimeMillis();
          boolean checkResult = JLibrustzcash.librustzcashSaplingFinalCheck(
              new LibrustzcashParam.FinalCheckParams(ctx, 0, bindingSig, signHash));
          long checkFinalCheckEndTime = System.currentTimeMillis();
          logger.info("parallel Transfer finalCheck cost is: " +
              (checkFinalCheckEndTime - checkFinalCheckStartTime) + "ms" +
              ", result is " + checkResult);
          result &= checkResult;
        } catch (Exception e) {
          result = false;
          logger.error("parallel check sign interrupted exception! ", e);
          Thread.currentThread().interrupt();
        } finally {
          JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
        }

      } else {
        try {
          result = JLibrustzcash.librustzcashSaplingCheckSpend(
              new LibrustzcashParam.CheckSpendParams(ctx, spendCv, anchor, nullifier, rk, spendProof, spendAuthSig, signHash));
          long checkSpendEndTime = System.currentTimeMillis();
          logger.info("Transfer checkSpend cost is: " + (checkSpendEndTime - verifyStartTime) + "ms");

          result &= JLibrustzcash.librustzcashSaplingCheckOutput(
              new LibrustzcashParam.CheckOutputParams(ctx, receiveCv0, cm[0], receiveEpk0, receiveProof0));
          long checkOutput1EndTime = System.currentTimeMillis();
          logger.info("Transfer checkOutput cost is: " + (checkOutput1EndTime - checkSpendEndTime) + "ms");

          result &= JLibrustzcash.librustzcashSaplingCheckOutput(
              new LibrustzcashParam.CheckOutputParams(ctx, receiveCv1, cm[1], receiveEpk1, receiveProof1));
          long checkOutput2EndTime = System.currentTimeMillis();
          logger.info("Transfer checkOutput cost is: " + (checkOutput2EndTime - checkOutput1EndTime) + "ms");

          result &= JLibrustzcash.librustzcashSaplingFinalCheck(
              new LibrustzcashParam.FinalCheckParams(ctx, 0, bindingSig, signHash));
          long checkFinalCheckEndTime = System.currentTimeMillis();
          logger.info("Transfer finalCheck cost is: " + (checkFinalCheckEndTime - checkOutput2EndTime) + "ms");

        } catch (Throwable any) {
          result = false;
        } finally {
          JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
        }
      }

      if (!result) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      long costTime = System.currentTimeMillis() - startTime;
      logger.info("Transfer verify successfully, " + "check cost is: " + costTime + "ms");

      long startTimeInsert = System.currentTimeMillis();
      Pair<Boolean, byte[]> pair = insertTwoleaves(frontier, cm, leafCount);
      costTime = System.currentTimeMillis() - startTimeInsert;
      logger.info("insertLeaf cost is: " + costTime + "ms");

      costTime = System.currentTimeMillis() - startTime;
      logger.info("Transfer pre-compile total cost is: " + costTime + "ms");

      return pair;

    }


    private Pair<Boolean, byte[]> checkBurn(byte[] data) {
      //spend
      byte[] cv = new byte[32];
      byte[] anchor = new byte[32];
      byte[] nullifier = new byte[32];
      byte[] rk = new byte[32];
      byte[] spendAuthSig = new byte[64];
      byte[] proof = new byte[192];

      byte[] bindingSig = new byte[64];
      byte[] signHash = new byte[32];
      //spend
      System.arraycopy(data, 0, cv, 0, 32);
      System.arraycopy(data, 32, rk, 0, 32);
      System.arraycopy(data, 64, spendAuthSig, 0, 64);
      System.arraycopy(data, 128, proof, 0, 192);
      System.arraycopy(data, 320, anchor, 0, 32);
      System.arraycopy(data, 352, nullifier, 0, 32);
      long value = parseLong(data, 384);
      System.arraycopy(data, 416, bindingSig, 0, 64);
      System.arraycopy(data, 480, signHash, 0, 32);

      boolean result;
      //verify spendProof && bindingSignature
      long ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
      try {
        result = JLibrustzcash.librustzcashSaplingCheckSpend(
                new LibrustzcashParam.CheckSpendParams(ctx, cv, anchor, nullifier, rk, proof, spendAuthSig, signHash));
        long valueBalance = value;
        result &= JLibrustzcash.librustzcashSaplingFinalCheck(
                new LibrustzcashParam.FinalCheckParams(ctx, valueBalance, bindingSig, signHash));
      } catch (Throwable any) {
        result = false;
      } finally {
        JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
      }
      logger.info("Burn verify successfully");
      return Pair.of(result, EMPTY_BYTE_ARRAY);

    }

    private long parseLong(byte[] data, int idx) {
      byte[] bytes = parseBytes(data, idx, 32);
      return new DataWord(bytes).longValueSafe();
    }

    private int getFrontierSlot(long leafIndex) {
      int slot = 0;
      if (leafIndex % 2 == 1) {
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

    private Pair<Boolean, byte[]> insertLeaf(byte[][] frontier, byte[] leafValue, long leafCount) {

      byte[] leftInput;
      byte[] rightInput;
      byte[] hash = new byte[32];
      byte[] nodeValue = new byte[32];

      int slot = getFrontierSlot(leafCount);
      long nodeIndex = leafCount + TREE_WIDTH - 1;
      System.arraycopy(leafValue, 0, nodeValue, 0, 32);

      logger.info("leafCount is " + leafCount + ", slot is " + slot);

      boolean success = true;
      byte[] result = new byte[(slot+2)*32+1];
      result[0] = (byte)slot;
      System.arraycopy(leafValue, 0, result, 1, 32);
      //compute root of Merkle Tree
      //TODO: optimization
      try {
        for (int level = 0; level < 32; level++) {
          if (level == slot) {
            System.arraycopy(nodeValue, 0, frontier[slot], 0, 32);

          }
          if (nodeIndex % 2 == 0) {
            leftInput = frontier[level];
            rightInput = nodeValue;

            nodeIndex = (nodeIndex - 1) / 2;
          } else {
            leftInput = nodeValue;
            rightInput = UNCOMMITTED[level];

            nodeIndex = nodeIndex / 2;
          }
          JLibrustzcash.librustzcashMerkleHash(new LibrustzcashParam.MerkleHashParams(level, leftInput, rightInput, hash));
          System.arraycopy(hash, 0, nodeValue, 0, 32);
          if (level < slot) {
            System.arraycopy(hash, 0, result, (level + 1) * 32 + 1, 32);
          }
        }

      } catch (Throwable any) {
        success = false;
      }
      if (!success) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      logger.info("Merkle root is " + ByteArray.toHexString(nodeValue));
      System.arraycopy(nodeValue, 0, result, (slot+1)*32+1, 32);
      return Pair.of(true, result);
    }

    private Pair<Boolean, byte[]> insertTwoleaves(byte[][] frontier, byte[][] leafValue, long leafCount) {

      long nodeIndex = 0;
      boolean success = true;
      int[] slot = new int[2];
      byte[] leftInput;
      byte[] rightInput;
      byte[] hash = new byte[32];
      byte[] nodeValue = new byte[32];

      slot[0] = getFrontierSlot(leafCount);
      slot[1] = getFrontierSlot(leafCount + 1);

      byte[] result = new byte[(slot[0] + slot[1] + 3) * 32 + 2];
      result[0] = (byte) (slot[0] & 0xFF);
      result[1] = (byte) (slot[1] & 0xFF);
      logger.info("slot count is " + (slot[0] + slot[1] + 2));

      // consider each new leaf in turn, from left to right:
      try {
        int resultIdx = 2;
        for (int i = 0; i < 2; i++) {
          nodeIndex = i + leafCount + TREE_WIDTH - 1; // convert the leafIndex to a nodeIndex
          logger.info("leaf index is " + (leafCount + i) + ", slot is " + slot[i]);

          System.arraycopy(leafValue[i], 0, nodeValue, 0, 32);
          System.arraycopy(leafValue[i], 0, result, resultIdx, 32);
          resultIdx += 32;
          if (slot[i] == 0) {
            System.arraycopy(nodeValue, 0, frontier[0], 0, 32);
            continue;
          }

          // hash up to the level whose nodeValue we'll store in the frontier slot:
          for (int level = 1; level <= slot[i]; level++) {
            if (nodeIndex % 2 == 0) {
              // even nodeIndex
              leftInput = frontier[level - 1];
              rightInput = nodeValue;

              nodeIndex = (nodeIndex - 1) / 2; // move one row up the tree
            } else {
              // odd nodeIndex
              leftInput = nodeValue;
              rightInput = UNCOMMITTED[level - 1];

              nodeIndex = nodeIndex / 2; // the parentIndex, but will become the nodeIndex of the next level
            }
            JLibrustzcash.librustzcashMerkleHash(new LibrustzcashParam.MerkleHashParams(level - 1, leftInput, rightInput, hash));
            System.arraycopy(hash, 0, nodeValue, 0, 32);
            System.arraycopy(hash, 0, result, resultIdx, 32);
            resultIdx += 32;
          }

          System.arraycopy(nodeValue, 0, frontier[slot[i]], 0, 32);// store in frontier
        }

        // So far we've added all leaves, and hashed up to a particular level of the tree.
        // We now need to continue hashing from that level until the root:
        for (int level = slot[1] + 1; level <= 32; level++) {

          if (nodeIndex % 2 == 0) {
            // even nodeIndex
            leftInput = frontier[level - 1];
            rightInput = nodeValue;

            nodeIndex = (nodeIndex - 1) / 2;  // the parentIndex, but will become the nodeIndex of the next level
          } else {
            // odd nodeIndex
            leftInput = nodeValue;
            rightInput = UNCOMMITTED[level - 1];

            nodeIndex = nodeIndex / 2;  // the parentIndex, but will become the nodeIndex of the next level
          }
          JLibrustzcash.librustzcashMerkleHash(new LibrustzcashParam.MerkleHashParams(level - 1, leftInput, rightInput, hash));
          System.arraycopy(hash, 0, nodeValue, 0, 32);
        }
        System.arraycopy(nodeValue, 0, result, resultIdx, 32);

        //logger.info("Result index is " + (resultIdx + 32));
      } catch (Throwable any) {
        success = false;
      }
      if (!success) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      logger.info("Merkle root is " + ByteArray.toHexString(nodeValue));
      return Pair.of(true, result);
    }
  }

  public static class CalTimeContract extends PrecompiledContract {

    private static final int CAL_SIZE = 32;

    @Override
    public long getEnergyForData(byte[] data) {
      return 0;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
       if (data.length == 0) {
        return getCurrentTime();
      } else {
        return calDeltaTime(data);
      }
    }

    private Pair<Boolean, byte[]> getCurrentTime() {
      long time = System.currentTimeMillis();
      logger.info("==== getCurrentTime get current is " + new DateTime(time) + ", " +  time);

      byte[] res = new byte[8];
      for(int i = 0; i < 8; i++) {
        res[i] = (byte)((time >> (i*8)) & 0xFF);
      }

      return Pair.of(true, res);
    }

    // print delta and return current
    private Pair<Boolean, byte[]> calDeltaTime(byte[] data) {
      long current = System.currentTimeMillis();

      long old = 0;
      for(int i = 7; i >= 0; i--){
        old = old << 8 | (data[i] & 0xFF);
      }

      long delta = current - old;

      logger.info("==== calDeltaTime old is " + new DateTime(old) + ", " + old);
      logger.info("==== calDeltaTime current is " + new DateTime(current) + ", " + current);
      logger.info("==== calDeltaTime delta is " + delta + "ms");

      byte[] res = new byte[8];
      for(int i = 0; i < 8; i++) {
        res[i] = (byte)((current >> (i*8)) & 0xFF);
      }
      return Pair.of(true, res);
    }
  }

  // compute pedersen hash
  public static class CalHash extends PrecompiledContract {

    @Override
    public long getEnergyForData(byte[] data) {
      return 0;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      byte[] left = new byte[32];
      byte[] right = new byte[32];
      byte[] hash = new byte[32];

      boolean res = true;
      try {
        int level = parseInt(data, 0);
        System.arraycopy(data, 32, left, 0, 32);
        System.arraycopy(data, 64, right, 0, 32);
        JLibrustzcash.librustzcashMerkleHash(new LibrustzcashParam.MerkleHashParams(level, left, right, hash));
        //System.arraycopy(hash, 0, preImage, 0, 32);
      } catch (Throwable any) {
        res = false;
      }
      if (!res){
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }
      return Pair.of(true, hash);

    }

    private int parseInt(byte[] data, int idx) {
      byte[] bytes = parseBytes(data, idx, 32);
      return new DataWord(bytes).intValueSafe();
    }
  }

}
