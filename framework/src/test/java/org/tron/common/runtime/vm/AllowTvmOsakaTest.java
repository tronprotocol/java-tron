package org.tron.common.runtime.vm;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
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
}
