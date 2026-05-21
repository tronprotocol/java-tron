package org.tron.common.runtime.vm;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.core.vm.PrecompiledContracts;

@Slf4j
public class P256VerifyTest {

  private static final PrecompiledContracts.P256Verify CONTRACT =
      new PrecompiledContracts.P256Verify();

  public static class TestCase {
    public String Input;
    public String Expected;
    public String Name;
    public int Gas;
    public boolean NoBenchmark;
  }

  private static byte[] hex(String s) {
    return ByteArray.fromHexString(s);
  }

  private static byte[] success() {
    byte[] r = new byte[32];
    r[31] = 0x01;
    return r;
  }

  @Test
  public void gethConformanceVectors() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    List<TestCase> cases;
    try (InputStream is = P256VerifyTest.class.getResourceAsStream(
        "/precompiles/p256verify_test_vectors.json")) {
      Assert.assertNotNull("test vectors resource missing", is);
      cases = mapper.readerForListOf(TestCase.class).readValue(is);
    }
    Assert.assertFalse("vector list empty", cases.isEmpty());

    for (TestCase tc : cases) {
      byte[] input = ByteArray.fromHexString(tc.Input);
      byte[] expected = tc.Expected == null || tc.Expected.isEmpty()
          ? new byte[0]
          : ByteArray.fromHexString(tc.Expected);

      Pair<Boolean, byte[]> result = CONTRACT.execute(input);

      Assert.assertTrue(tc.Name + ": precompile must not revert", result.getLeft());
      Assert.assertArrayEquals(tc.Name + ": output mismatch",
          expected, result.getRight());
      Assert.assertEquals(tc.Name + ": gas mismatch",
          tc.Gas, CONTRACT.getEnergyForData(input));
    }
  }

  @Test
  public void rejectsNullInput() {
    Pair<Boolean, byte[]> r = CONTRACT.execute(null);
    Assert.assertTrue(r.getLeft());
    Assert.assertArrayEquals(new byte[0], r.getRight());
  }

  @Test
  public void rejectsEmptyInput() {
    Pair<Boolean, byte[]> r = CONTRACT.execute(new byte[0]);
    Assert.assertTrue(r.getLeft());
    Assert.assertArrayEquals(new byte[0], r.getRight());
  }

  @Test
  public void rejectsShortInput() {
    Pair<Boolean, byte[]> r = CONTRACT.execute(new byte[159]);
    Assert.assertTrue(r.getLeft());
    Assert.assertArrayEquals(new byte[0], r.getRight());
  }

  @Test
  public void rejectsLongInput() {
    Pair<Boolean, byte[]> r = CONTRACT.execute(new byte[161]);
    Assert.assertTrue(r.getLeft());
    Assert.assertArrayEquals(new byte[0], r.getRight());
  }

  @Test
  public void rejectsInfinityPoint() {
    // Valid h, r, s plus qx=qy=0 -> infinity-encoded public key.
    String input =
        "4cee90eb86eaa050036147a12d49004b6b9c72bd725d39d4785011fe190f0b4d"
            + "a73bd4903f0ce3b639bbbf6e8e80d16931ff4bcf5993d58468e8fb19086e8cac"
            + "36dbcd03009df8c59286b162af3bd7fcc0450c9aa81be5d10d312af6c66b1d60"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000";
    Pair<Boolean, byte[]> r = CONTRACT.execute(hex(input));
    Assert.assertTrue(r.getLeft());
    Assert.assertArrayEquals(new byte[0], r.getRight());
  }

  /**
   * Public key coordinates are valid field elements but the point is NOT on
   * the secp256r1 curve (they happen to be the secp256k1 base point). The
   * precompile must fail the on-curve check before attempting verification.
   * Input lifted from Besu's P256VerifyPrecompiledContractTest.
   */
  @Test
  public void rejectsOffCurvePoint() {
    String input =
        "44acf6b7e36c1342c2c5897204fe09504e1e2efb1a900377dbc4e7a6a133ec56"
            + "c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5"
            + "30dae23890abb63e378e003d7f1d5006ab23cc7b3b65b3d0c7b45c7e1e2e08b9"
            + "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"
            + "b7c52588d95c3b9aa25b0403f1eef75702e84bb7597aabe663b82f6f04ef2777";
    Pair<Boolean, byte[]> r = CONTRACT.execute(hex(input));
    Assert.assertTrue(r.getLeft());
    Assert.assertArrayEquals(new byte[0], r.getRight());
  }

  /**
   * The recovered point's x-coordinate exceeds n; verification must still
   * succeed because R'.x mod n == r. Input lifted from Besu's
   * testModularComparisonWhenRPrimeExceedsN.
   */
  @Test
  public void acceptsModularComparisonWhenRPrimeExceedsN() {
    String input =
        "BB5A52F42F9C9261ED4361F59422A1E30036E7C32B270C8807A419FECA605023"
            + "000000000000000000000000000000004319055358E8617B0C46353D039CDAAB"
            + "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC63254E"
            + "0AD99500288D466940031D72A9F5445A4D43784640855BF0A69874D2DE5FE103"
            + "C5011E6EF2C42DCD50D5D3D29F99AE6EBA2C80C9244F4C5422F0979FF0C3BA5E";
    Pair<Boolean, byte[]> r = CONTRACT.execute(hex(input));
    Assert.assertTrue(r.getLeft());
    Assert.assertArrayEquals(success(), r.getRight());
  }

  @Test
  public void gasCostIsConstant6900() {
    Assert.assertEquals(6900L, CONTRACT.getEnergyForData(null));
    Assert.assertEquals(6900L, CONTRACT.getEnergyForData(new byte[0]));
    Assert.assertEquals(6900L, CONTRACT.getEnergyForData(new byte[160]));
    Assert.assertEquals(6900L, CONTRACT.getEnergyForData(new byte[1024]));
  }
}
