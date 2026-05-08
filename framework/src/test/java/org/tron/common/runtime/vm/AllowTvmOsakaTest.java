package org.tron.common.runtime.vm;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.math.StrictMathWrapper;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.vm.PrecompiledContracts;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;

@Slf4j
public class AllowTvmOsakaTest extends VMTestBase {

  private static final PrecompiledContracts.PrecompiledContract modExp =
      new PrecompiledContracts.ModExp();

  private static byte[] toLenBytes(int value) {
    byte[] b = new byte[32];
    b[28] = (byte) ((value >> 24) & 0xFF);
    b[29] = (byte) ((value >> 16) & 0xFF);
    b[30] = (byte) ((value >> 8) & 0xFF);
    b[31] = (byte) (value & 0xFF);
    return b;
  }

  @Test
  public void testEIP7823() {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmOsaka(1);

    try {
      // all-zero lengths: should succeed
      Pair<Boolean, byte[]> result = modExp.execute(
          ByteUtil.merge(toLenBytes(0), toLenBytes(0), toLenBytes(0)));
      Assert.assertTrue(result.getLeft());

      // baseLen == 1024: boundary, should succeed
      result = modExp.execute(
          ByteUtil.merge(toLenBytes(1024), toLenBytes(0), toLenBytes(0)));
      Assert.assertTrue(result.getLeft());

      // baseLen == 1025: just over the limit, should fail
      result = modExp.execute(
          ByteUtil.merge(toLenBytes(1025), toLenBytes(0), toLenBytes(0)));
      Assert.assertFalse(result.getLeft());

      // oversized expLen only: should fail
      result = modExp.execute(
          ByteUtil.merge(toLenBytes(0), toLenBytes(1025), toLenBytes(0)));
      Assert.assertFalse(result.getLeft());

      // oversized modLen only: should fail
      result = modExp.execute(
          ByteUtil.merge(toLenBytes(0), toLenBytes(0), toLenBytes(1025)));
      Assert.assertFalse(result.getLeft());
    } finally {
      VMConfig.initAllowTvmOsaka(0);
      ConfigLoader.disable = false;
    }
  }

  /**
   * Build ModExp input data for energy calculation testing.
   */
  private static byte[] buildModExpData(int baseLen, int expLen, int modLen, byte[] expValue) {
    byte[] base = new byte[baseLen];
    byte[] exp = new byte[expLen];
    if (expValue.length > 0 && expLen > 0) {
      System.arraycopy(expValue, 0, exp, 0, StrictMathWrapper.min(expValue.length, expLen));
    }
    byte[] mod = new byte[modLen];
    return ByteUtil.merge(toLenBytes(baseLen), toLenBytes(expLen), toLenBytes(modLen),
        base, exp, mod);
  }

  private static long getEnergy(int baseLen, int expLen, int modLen, byte[] expValue) {
    return modExp.getEnergyForData(buildModExpData(baseLen, expLen, modLen, expValue));
  }

  @Test
  public void testEIP7883ModExpPricing() {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmOsaka(1);

    try {
      byte[] square = {0x02};
      byte[] qube = {0x03};
      byte[] pow0x10001 = {0x01, 0x00, 0x01};

      // nagydani_1: baseLen=64, expLen=square/qube:1 pow:3, modLen=64
      Assert.assertEquals(500L, getEnergy(64, 1, 64, square));
      Assert.assertEquals(500L, getEnergy(64, 1, 64, qube));
      Assert.assertEquals(2048L, getEnergy(64, 3, 64, pow0x10001));

      // nagydani_2: baseLen=128, modLen=128
      Assert.assertEquals(512L, getEnergy(128, 1, 128, square));
      Assert.assertEquals(512L, getEnergy(128, 1, 128, qube));
      Assert.assertEquals(8192L, getEnergy(128, 3, 128, pow0x10001));

      // nagydani_3: baseLen=256, modLen=256
      Assert.assertEquals(2048L, getEnergy(256, 1, 256, square));
      Assert.assertEquals(2048L, getEnergy(256, 1, 256, qube));
      Assert.assertEquals(32768L, getEnergy(256, 3, 256, pow0x10001));

      // nagydani_4: baseLen=512, modLen=512
      Assert.assertEquals(8192L, getEnergy(512, 1, 512, square));
      Assert.assertEquals(8192L, getEnergy(512, 1, 512, qube));
      Assert.assertEquals(131072L, getEnergy(512, 3, 512, pow0x10001));

      // nagydani_5: baseLen=1024, modLen=1024
      Assert.assertEquals(32768L, getEnergy(1024, 1, 1024, square));
      Assert.assertEquals(32768L, getEnergy(1024, 1, 1024, qube));
      Assert.assertEquals(524288L, getEnergy(1024, 3, 1024, pow0x10001));

      // Minimum energy: zero-length inputs
      Assert.assertEquals(500L, getEnergy(0, 0, 0, new byte[]{}));

      // Small base/mod (<=32): complexity=16
      Assert.assertEquals(500L, getEnergy(1, 1, 1, square));
      Assert.assertEquals(500L, getEnergy(32, 1, 32, square));

      // Boundary: base/mod at 33 (just over 32) uses doubled formula
      // words = ceil(33/8) = 5, complexity = 2 * 25 = 50, iterCount = 1
      Assert.assertEquals(500L, getEnergy(33, 1, 33, square));

      // Same boundary with expLen=64 forces a non-floor result so the
      // 2*words² branch is observable: complexity=50, iterCount=16*(64-32)=512,
      // energy = 50 * 512 = 25600.
      Assert.assertEquals(25600L, getEnergy(33, 64, 33, new byte[]{}));

      // Exponent > 32 bytes: multiplier is 16
      // expLen=64, high bytes all zero → highestBit=0, iterCount = 16*(64-32)+0 = 512
      // baseLen=64, modLen=64 → complexity=128, energy=128*512=65536
      Assert.assertEquals(65536L, getEnergy(64, 64, 64, new byte[]{}));

      // Exponent > 32 bytes with non-zero high bytes
      // expLen=64, first byte=0x01 → highestBit=248, iterCount = 16*32+248 = 760
      // baseLen=64, modLen=64 → complexity=128, energy=128*760=97280
      Assert.assertEquals(97280L, getEnergy(64, 64, 64, new byte[]{0x01}));
    } finally {
      VMConfig.initAllowTvmOsaka(0);
      ConfigLoader.disable = false;
    }
  }

  @Test
  public void testEIP7883DisabledPreservesOldPricing() {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmOsaka(0);

    try {
      // When Osaka is disabled, the existing EIP-198-style pricing formula is used:
      // nagydani_1_square: 4096 * 1 / 20 = 204.
      long energy = getEnergy(64, 1, 64, new byte[]{0x02});
      Assert.assertEquals(204L, energy);
    } finally {
      ConfigLoader.disable = false;
    }
  }

  @Test
  public void testEIP7883CanBeLowerThanLegacyPricing() {
    ConfigLoader.disable = true;

    try {
      byte[] square = {0x02};

      VMConfig.initAllowTvmOsaka(0);
      Assert.assertEquals(665L, getEnergy(128, 1, 128, square));

      VMConfig.initAllowTvmOsaka(1);
      Assert.assertEquals(512L, getEnergy(128, 1, 128, square));
    } finally {
      VMConfig.initAllowTvmOsaka(0);
      ConfigLoader.disable = false;
    }
  }

  @Test
  public void testEIP7823DisabledShouldPass() {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmOsaka(0);

    try {
      // all limits exceeded while osaka is disabled: should succeed (no restriction)
      Pair<Boolean, byte[]> result = modExp.execute(
          ByteUtil.merge(toLenBytes(2048), toLenBytes(2048), toLenBytes(2048)));
      Assert.assertTrue(result.getLeft());
    } finally {
      ConfigLoader.disable = false;
    }
  }

  // P256VERIFY address per TIP-7951 / EIP-7951.
  private static final DataWord P256_VERIFY_ADDR =
      new DataWord(
          "0000000000000000000000000000000000000000000000000000000000000100");

  // First entry from the geth conformance vectors — known-valid signature.
  private static final String VALID_P256_INPUT =
      "4cee90eb86eaa050036147a12d49004b6b9c72bd725d39d4785011fe190f0b4d"
          + "a73bd4903f0ce3b639bbbf6e8e80d16931ff4bcf5993d58468e8fb19086e8cac"
          + "36dbcd03009df8c59286b162af3bd7fcc0450c9aa81be5d10d312af6c66b1d60"
          + "4aebd3099c618202fcfe16ae7770b0c49ab5eadf74b754204a3bb6060e44eff3"
          + "7618b065f9832de4ca6ca971a7a1adc826d0f7c00181a5fb2ddf79ae00b4e10e";
  private static final byte[] EXPECTED_VALID_OUTPUT = ByteUtil.merge(
      new byte[31], new byte[]{0x01});

  @Test
  public void testP256VerifyEnabled() {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmOsaka(1);
    try {
      PrecompiledContracts.PrecompiledContract contract =
          PrecompiledContracts.getContractForAddress(P256_VERIFY_ADDR);
      Assert.assertNotNull("P256VERIFY must be registered when osaka is on",
          contract);
      Assert.assertTrue(contract instanceof PrecompiledContracts.P256Verify);

      Pair<Boolean, byte[]> result = contract.execute(
          ByteArray.fromHexString(VALID_P256_INPUT));
      Assert.assertTrue(result.getLeft());
      Assert.assertArrayEquals(EXPECTED_VALID_OUTPUT, result.getRight());
      Assert.assertEquals(6900L, contract.getEnergyForData(
          ByteArray.fromHexString(VALID_P256_INPUT)));
    } finally {
      VMConfig.initAllowTvmOsaka(0);
      ConfigLoader.disable = false;
    }
  }

  @Test
  public void testP256VerifyDisabledShouldReturnNull() {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmOsaka(0);
    try {
      Assert.assertNull(
          "P256VERIFY must NOT be registered when osaka is off",
          PrecompiledContracts.getContractForAddress(P256_VERIFY_ADDR));
    } finally {
      ConfigLoader.disable = false;
    }
  }
}
