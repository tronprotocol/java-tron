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

  @Test
  public void testEIP7823() {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmOsaka(1);

    try {
      byte[] baseLen = new byte[32];
      byte[] expLen = new byte[32];
      byte[] modLen = new byte[32];

      PrecompiledContracts.PrecompiledContract modExp = new PrecompiledContracts.ModExp();

      // Valid lens: all zeros (0 <= 1024)
      Pair<Boolean, byte[]> result = modExp.execute(ByteUtil.merge(baseLen, expLen, modLen));
      Assert.assertTrue(result.getLeft());

      // Invalid lens: baseLen = 0x01000000... = 16777216 > 1024
      baseLen[0] = 0x01;
      result = modExp.execute(ByteUtil.merge(baseLen, expLen, modLen));
      Assert.assertFalse(result.getLeft());
    } finally {
      VMConfig.initAllowTvmOsaka(0);
      ConfigLoader.disable = false;
    }
  }
}
