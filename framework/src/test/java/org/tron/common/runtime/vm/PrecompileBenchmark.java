package org.tron.common.runtime.vm;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.ByteArray;
import org.tron.core.vm.PrecompiledContracts;

/**
 * Manual microbenchmarks comparing the ECRecover (3000 gas) precompile
 * against the new P256VERIFY (6900 gas) precompile from TIP-7951. Not part
 * of the regular test suite. It is opt-in behind a system property:
 *
 *   ./gradlew :framework:test -DrunPrecompileBenchmark=true --tests \
 *       org.tron.common.runtime.vm.PrecompileBenchmark -i
 *
 * Four @Test methods, each independent:
 *   - compareEcrecoverVsP256:  baseline timing, single fixed input.
 *   - p256FailPaths:           per-validation-step timing, confirms early
 *                              returns short-circuit before ECDSA math.
 *   - compareDiverseInputs:    rotates over N distinct keypairs to defeat
 *                              any per-key caching and branch-predictor bias.
 *   - coldNoWarmup:            no execute() warmup, distinct input each call, first
 *                              100 calls bucketed — closer to the mainnet
 *                              case where P256VERIFY is invoked rarely and
 *                              the JVM has not JIT-compiled the path yet.
 *
 * Single-threaded, pure-Java BouncyCastle path. The first three tests use a
 * 5000-iteration execute() warmup; coldNoWarmup deliberately skips it. Test
 * input generation happens before timing and may load cryptographic helper code.
 */
public class PrecompileBenchmark {

  private static final String RUN_PROPERTY = "runPrecompileBenchmark";
  private static final int WARMUP_ITERS = 5_000;
  private static final int MEASURE_ITERS = 5_000;
  private static final int ROUNDS = 5;
  private static final int DIVERSE_KEYS = 100;

  private static final PrecompiledContracts.ECRecover EC_RECOVER =
      new PrecompiledContracts.ECRecover();
  private static final PrecompiledContracts.P256Verify P256_VERIFY =
      new PrecompiledContracts.P256Verify();

  @Before
  public void requireExplicitOptIn() {
    Assume.assumeTrue("set -D" + RUN_PROPERTY + "=true to run manual benchmarks",
        Boolean.getBoolean(RUN_PROPERTY));
  }

  // First entry from go-ethereum's EIP-7951 conformance vectors — known-valid.
  private static final String VALID_P256_INPUT =
      "4cee90eb86eaa050036147a12d49004b6b9c72bd725d39d4785011fe190f0b4d"
          + "a73bd4903f0ce3b639bbbf6e8e80d16931ff4bcf5993d58468e8fb19086e8cac"
          + "36dbcd03009df8c59286b162af3bd7fcc0450c9aa81be5d10d312af6c66b1d60"
          + "4aebd3099c618202fcfe16ae7770b0c49ab5eadf74b754204a3bb6060e44eff3"
          + "7618b065f9832de4ca6ca971a7a1adc826d0f7c00181a5fb2ddf79ae00b4e10e";

  private static final String P256_N_HEX =
      "ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551";
  private static final String P256_P_HEX =
      "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff";

  // Public key (qx, qy) coordinates that are valid field elements but not on
  // secp256r1 — they are the secp256k1 base point. From Besu's test suite.
  private static final String P256_OFF_CURVE_INPUT =
      "44acf6b7e36c1342c2c5897204fe09504e1e2efb1a900377dbc4e7a6a133ec56"
          + "c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5"
          + "30dae23890abb63e378e003d7f1d5006ab23cc7b3b65b3d0c7b45c7e1e2e08b9"
          + "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"
          + "b7c52588d95c3b9aa25b0403f1eef75702e84bb7597aabe663b82f6f04ef2777";

  private static byte[] toFixed32(BigInteger x) {
    byte[] raw = x.toByteArray();
    byte[] out = new byte[32];
    if (raw.length == 33 && raw[0] == 0) {
      System.arraycopy(raw, 1, out, 0, 32);
    } else {
      System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
    }
    return out;
  }

  /** Build a valid 128-byte ECRecover input: hash(32) | v(32 padded) | r(32) | s(32). */
  private static byte[] buildEcrecoverInput(byte[] hash, ECKey key) {
    ECDSASignature sig = key.sign(hash);
    byte[] input = new byte[128];
    System.arraycopy(hash, 0, input, 0, 32);
    input[63] = sig.v;
    System.arraycopy(toFixed32(sig.r), 0, input, 64, 32);
    System.arraycopy(toFixed32(sig.s), 0, input, 96, 32);
    return input;
  }

  /** Generate N distinct valid 128-byte ECRecover inputs (fresh ECKey each). */
  private static byte[][] buildEcrecoverInputs(int n) {
    SecureRandom random = new SecureRandom();
    byte[][] result = new byte[n][];
    for (int i = 0; i < n; i++) {
      byte[] hash = new byte[32];
      random.nextBytes(hash);
      result[i] = buildEcrecoverInput(hash, new ECKey());
    }
    return result;
  }

  /** Generate N distinct valid 160-byte P256VERIFY inputs (fresh keypair each). */
  private static byte[][] buildP256Inputs(int n) {
    X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
    ECDomainParameters domain = new ECDomainParameters(
        curve.getCurve(), curve.getG(), curve.getN(), curve.getH());
    SecureRandom random = new SecureRandom();
    ECKeyPairGenerator gen = new ECKeyPairGenerator();
    gen.init(new ECKeyGenerationParameters(domain, random));

    byte[][] result = new byte[n][];
    for (int i = 0; i < n; i++) {
      AsymmetricCipherKeyPair pair = gen.generateKeyPair();
      ECPrivateKeyParameters priv = (ECPrivateKeyParameters) pair.getPrivate();
      ECPublicKeyParameters pub = (ECPublicKeyParameters) pair.getPublic();

      byte[] hash = new byte[32];
      random.nextBytes(hash);

      ECDSASigner signer = new ECDSASigner();
      signer.init(true, priv);
      BigInteger[] sig = signer.generateSignature(hash);

      org.bouncycastle.math.ec.ECPoint q = pub.getQ().normalize();
      BigInteger qx = q.getAffineXCoord().toBigInteger();
      BigInteger qy = q.getAffineYCoord().toBigInteger();

      byte[] input = new byte[160];
      System.arraycopy(hash, 0, input, 0, 32);
      System.arraycopy(toFixed32(sig[0]), 0, input, 32, 32);
      System.arraycopy(toFixed32(sig[1]), 0, input, 64, 32);
      System.arraycopy(toFixed32(qx), 0, input, 96, 32);
      System.arraycopy(toFixed32(qy), 0, input, 128, 32);
      result[i] = input;
    }
    return result;
  }

  /**
   * Returns total elapsed nanos. Accumulates the always-true left of the
   * Pair to prevent dead-code elimination without depending on output size
   * (so it works for both valid and invalid input benches).
   */
  private static long bench(PrecompiledContracts.PrecompiledContract contract,
                            byte[] input,
                            int iters) {
    long acc = 0;
    long start = System.nanoTime();
    for (int i = 0; i < iters; i++) {
      Pair<Boolean, byte[]> r = contract.execute(input);
      acc += r.getLeft() ? 1 : 0;
      acc += r.getRight().length;
    }
    long elapsed = System.nanoTime() - start;
    if (acc <= 0) {
      throw new AssertionError("benchmark sanity: zero accumulator");
    }
    return elapsed;
  }

  /** Variant of bench() that rotates over a pool of distinct inputs. */
  private static long benchRotating(PrecompiledContracts.PrecompiledContract contract,
                                    byte[][] inputs,
                                    int iters) {
    long acc = 0;
    int n = inputs.length;
    long start = System.nanoTime();
    for (int i = 0; i < iters; i++) {
      Pair<Boolean, byte[]> r = contract.execute(inputs[i % n]);
      acc += r.getLeft() ? 1 : 0;
      acc += r.getRight().length;
    }
    long elapsed = System.nanoTime() - start;
    if (acc <= 0) {
      throw new AssertionError("benchRotating sanity: zero accumulator");
    }
    return elapsed;
  }

  /** ============================== TEST 1 ============================== */

  @Test
  public void compareEcrecoverVsP256() {
    byte[] ecInput = buildEcrecoverInput(deterministicHash(0xA), new ECKey());
    byte[] p256Input = ByteArray.fromHexString(VALID_P256_INPUT);

    if (EC_RECOVER.execute(ecInput).getRight().length == 0) {
      throw new AssertionError("ecrecover sanity: empty output");
    }
    if (P256_VERIFY.execute(p256Input).getRight().length == 0) {
      throw new AssertionError("p256verify sanity: empty output");
    }

    bench(EC_RECOVER, ecInput, WARMUP_ITERS);
    bench(P256_VERIFY, p256Input, WARMUP_ITERS);

    long ecNanos = 0;
    long p256Nanos = 0;
    StringBuilder rounds = new StringBuilder();
    for (int round = 0; round < ROUNDS; round++) {
      long ec = bench(EC_RECOVER, ecInput, MEASURE_ITERS);
      long p256 = bench(P256_VERIFY, p256Input, MEASURE_ITERS);
      ecNanos += ec;
      p256Nanos += p256;
      rounds.append(String.format(
          "    round %d/%d:  ec %8.0f ns/op   p256 %8.0f ns/op%n",
          round + 1, ROUNDS,
          (double) ec / MEASURE_ITERS,
          (double) p256 / MEASURE_ITERS));
    }
    long total = (long) ROUNDS * MEASURE_ITERS;
    double ecNs = (double) ecNanos / total;
    double p256Ns = (double) p256Nanos / total;

    System.out.printf(
        "%n=== TEST 1: baseline single-input (warmup %d, measure %d x %d) ===%n%s"
            + "  ECRecover  (3000 gas) : %8.0f ns/op   %8.0f ops/s%n"
            + "  P256Verify (6900 gas) : %8.0f ns/op   %8.0f ops/s%n"
            + "  P256 / EC time ratio  : %.2fx     (gas ratio: 2.30x)%n",
        WARMUP_ITERS, MEASURE_ITERS, ROUNDS, rounds.toString(),
        ecNs, 1e9 / ecNs,
        p256Ns, 1e9 / p256Ns,
        p256Ns / ecNs);
  }

  /** ============================== TEST 2 ============================== */

  @Test
  public void p256FailPaths() {
    byte[] valid = ByteArray.fromHexString(VALID_P256_INPUT);

    byte[] tooShort = new byte[159];

    byte[] rEqualsN = valid.clone();
    System.arraycopy(ByteArray.fromHexString(P256_N_HEX), 0, rEqualsN, 32, 32);

    byte[] qxEqualsP = valid.clone();
    System.arraycopy(ByteArray.fromHexString(P256_P_HEX), 0, qxEqualsP, 96, 32);

    byte[] infinity = valid.clone();
    Arrays.fill(infinity, 96, 160, (byte) 0);

    byte[] offCurve = ByteArray.fromHexString(P256_OFF_CURVE_INPUT);

    byte[] badSig = valid.clone();
    badSig[0] ^= 0x01; // perturbing the message hash makes ECDSA verify fail

    String[] names = {
        "1. len!=160         ",
        "2. r=N (bound)      ",
        "3. qx=P (bound)     ",
        "4. (qx,qy)=(0,0)    ",
        "5. point off-curve  ",
        "6. ECDSA verify fail",
        "0. VALID full pass  ",
    };
    byte[][] inputs = {tooShort, rEqualsN, qxEqualsP, infinity, offCurve, badSig, valid};

    // sanity: every fail-case returns empty, the valid case returns 32 bytes.
    for (int i = 0; i < inputs.length; i++) {
      int len = P256_VERIFY.execute(inputs[i]).getRight().length;
      boolean expectEmpty = i < 6;
      if (expectEmpty && len != 0) {
        throw new AssertionError("setup: expected empty for " + names[i].trim()
            + " but got len=" + len);
      }
      if (!expectEmpty && len != 32) {
        throw new AssertionError("setup: expected len=32 for VALID but got " + len);
      }
    }

    for (byte[] in : inputs) {
      bench(P256_VERIFY, in, WARMUP_ITERS);
    }

    System.out.printf("%n=== TEST 2: P256 fail-path timing (measure %d x %d) ===%n",
        MEASURE_ITERS, ROUNDS);
    for (int i = 0; i < inputs.length; i++) {
      long ns = 0;
      for (int r = 0; r < ROUNDS; r++) {
        ns += bench(P256_VERIFY, inputs[i], MEASURE_ITERS);
      }
      double nsOp = (double) ns / ((long) ROUNDS * MEASURE_ITERS);
      System.out.printf("  %s : %10.0f ns/op   %10.0f ops/s%n",
          names[i], nsOp, 1e9 / nsOp);
    }
  }

  /** ============================== TEST 3 ============================== */

  @Test
  public void compareDiverseInputs() {
    int n = DIVERSE_KEYS;
    byte[][] ecInputs = buildEcrecoverInputs(n);
    byte[][] p256Inputs = buildP256Inputs(n);

    for (byte[] in : ecInputs) {
      if (EC_RECOVER.execute(in).getRight().length == 0) {
        throw new AssertionError("ec rotating sanity: empty output");
      }
    }
    for (byte[] in : p256Inputs) {
      if (P256_VERIFY.execute(in).getRight().length == 0) {
        throw new AssertionError("p256 rotating sanity: empty output");
      }
    }

    benchRotating(EC_RECOVER, ecInputs, WARMUP_ITERS);
    benchRotating(P256_VERIFY, p256Inputs, WARMUP_ITERS);

    long ecNanos = 0;
    long p256Nanos = 0;
    StringBuilder rounds = new StringBuilder();
    for (int round = 0; round < ROUNDS; round++) {
      long ec = benchRotating(EC_RECOVER, ecInputs, MEASURE_ITERS);
      long p256 = benchRotating(P256_VERIFY, p256Inputs, MEASURE_ITERS);
      ecNanos += ec;
      p256Nanos += p256;
      rounds.append(String.format(
          "    round %d/%d:  ec %8.0f ns/op   p256 %8.0f ns/op%n",
          round + 1, ROUNDS,
          (double) ec / MEASURE_ITERS,
          (double) p256 / MEASURE_ITERS));
    }
    long total = (long) ROUNDS * MEASURE_ITERS;
    double ecNs = (double) ecNanos / total;
    double p256Ns = (double) p256Nanos / total;

    System.out.printf(
        "%n=== TEST 3: diverse-input rotation (%d distinct keys, measure %d x %d) ===%n%s"
            + "  ECRecover  (rotating) : %8.0f ns/op   %8.0f ops/s%n"
            + "  P256Verify (rotating) : %8.0f ns/op   %8.0f ops/s%n"
            + "  P256 / EC time ratio  : %.2fx     (gas ratio: 2.30x)%n",
        n, MEASURE_ITERS, ROUNDS, rounds.toString(),
        ecNs, 1e9 / ecNs,
        p256Ns, 1e9 / p256Ns,
        p256Ns / ecNs);
  }

  private static byte[] deterministicHash(int seed) {
    byte[] hash = new byte[32];
    for (int i = 0; i < 32; i++) {
      hash[i] = (byte) ((i * 7 + seed) & 0xff);
    }
    return hash;
  }

  /** ============================== TEST 4 ============================== */

  /**
   * Cold no-warmup measurement. Skips the {@code WARMUP_ITERS} execute()
   * prelude so the first measured precompile call sees an unprimed execute path
   * — closer to the TRON mainnet scenario where P256VERIFY is invoked at low
   * frequency and the precompile path rarely reaches C2 steady state.
   *
   * <p>Reports the first call alone plus bucketed averages over the first 100
   * calls so the JIT promotion curve is visible. Each call uses a distinct
   * input (fresh keypair / signature) to defeat any per-input caching. Inputs
   * are generated before timing, so this does not measure cryptographic helper
   * classloading performed by key generation.
   *
   * <p>For the coldest execute() measurement, run this test alone in a fresh JVM:
   *
   *   ./gradlew :framework:test --no-daemon -DrunPrecompileBenchmark=true --tests \
   *       'org.tron.common.runtime.vm.PrecompileBenchmark.coldNoWarmup' -i
   *
   * Otherwise the other @Test methods running first will already have
   * JIT-compiled {@code execute()} and the early buckets will be artificially
   * fast.
   */
  @Test
  public void coldNoWarmup() {
    int n = 100;
    byte[][] p256Inputs = buildP256Inputs(n);
    byte[][] ecInputs = buildEcrecoverInputs(n);

    System.gc();

    long acc = 0;
    long[] p256Nanos = new long[n];
    for (int i = 0; i < n; i++) {
      long start = System.nanoTime();
      Pair<Boolean, byte[]> r = P256_VERIFY.execute(p256Inputs[i]);
      p256Nanos[i] = System.nanoTime() - start;
      acc += r.getLeft() ? 1 : 0;
      acc += r.getRight().length;
    }

    long[] ecNanos = new long[n];
    for (int i = 0; i < n; i++) {
      long start = System.nanoTime();
      Pair<Boolean, byte[]> r = EC_RECOVER.execute(ecInputs[i]);
      ecNanos[i] = System.nanoTime() - start;
      acc += r.getLeft() ? 1 : 0;
      acc += r.getRight().length;
    }
    if (acc <= 0) {
      throw new AssertionError("coldNoWarmup sanity: zero accumulator");
    }

    System.out.printf(
        "%n=== TEST 4: cold no-warmup (distinct inputs, no JIT priming) ===%n"
            + "  P256Verify (6900 gas):%n");
    reportBucket("    call #1            ", p256Nanos, 0, 1);
    reportBucket("    calls #2..10  (avg)", p256Nanos, 1, 10);
    reportBucket("    calls #11..100(avg)", p256Nanos, 10, 100);
    System.out.printf("  ECRecover  (3000 gas):%n");
    reportBucket("    call #1            ", ecNanos, 0, 1);
    reportBucket("    calls #2..10  (avg)", ecNanos, 1, 10);
    reportBucket("    calls #11..100(avg)", ecNanos, 10, 100);
  }

  private static void reportBucket(String label, long[] nanos, int from, int to) {
    long sum = 0;
    for (int i = from; i < to; i++) {
      sum += nanos[i];
    }
    double nsOp = (double) sum / (to - from);
    System.out.printf("%s : %10.0f ns/op   %10.0f ops/s%n",
        label, nsOp, 1e9 / nsOp);
  }
}
