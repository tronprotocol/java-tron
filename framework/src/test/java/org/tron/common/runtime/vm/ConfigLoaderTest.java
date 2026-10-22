package org.tron.common.runtime.vm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.util.BigIntegers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.tron.common.VMConfigRule;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.PrecompiledContracts;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;

@RunWith(Parameterized.class)
public class ConfigLoaderTest {

  @Rule
  public final VMConfigRule vmConfigRule = new VMConfigRule();

  private final long propertyValue;

  public ConfigLoaderTest(long propertyValue) {
    this.propertyValue = propertyValue;
  }

  @Parameterized.Parameters(name = "strictEcdsa={0}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][]{{0L}, {1L}});
  }

  @Before
  public void enableLoader() {
    ConfigLoader.disable = false;
  }

  @Test
  public void shouldLoadSharedRecoveryModeAndClearLocalOverride() throws Exception {
    ConfigLoader.load(stores(1 - propertyValue), false);
    VMConfig.Snapshot local = new VMConfig.Snapshot();
    local.allowStrictEcdsaValidation = propertyValue == 0;
    VMConfig.setLocalSnapshot(local);

    ConfigLoader.load(stores(propertyValue), false);

    assertRecoveryMode(propertyValue == 1);
    assertGlobalRecoveryMode(propertyValue == 1);
    VMConfig.clearLocalSnapshot();
    assertRecoveryMode(propertyValue == 1);
  }

  @Test
  public void shouldLoadIsolatedRecoveryModeWithoutChangingGlobal() throws Exception {
    ConfigLoader.load(stores(1 - propertyValue), false);
    assertRecoveryMode(propertyValue == 0);

    ConfigLoader.load(stores(propertyValue), true);

    assertRecoveryMode(propertyValue == 1);
    assertGlobalRecoveryMode(propertyValue == 0);
    VMConfig.clearLocalSnapshot();
    assertRecoveryMode(propertyValue == 0);
  }

  private StoreFactory stores(long value) {
    DynamicPropertiesStore properties = mock(DynamicPropertiesStore.class);
    when(properties.getAllowStrictEcdsaValidation()).thenReturn(value);
    when(properties.allowStrictEcdsaValidation()).thenCallRealMethod();
    ChainBaseManager manager = mock(ChainBaseManager.class);
    when(manager.getDynamicPropertiesStore()).thenReturn(properties);
    StoreFactory factory = mock(StoreFactory.class);
    when(factory.getChainBaseManager()).thenReturn(manager);
    return factory;
  }

  private void assertGlobalRecoveryMode(boolean strict) throws Exception {
    ExecutorService worker = Executors.newSingleThreadExecutor();
    try {
      // A separate thread has no local snapshot and observes the shared configuration.
      worker.submit(() -> assertRecoveryMode(strict)).get(5, TimeUnit.SECONDS);
    } finally {
      worker.shutdownNow();
      assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private void assertRecoveryMode(boolean strict) {
    assertEquals(strict, VMConfig.allowStrictEcdsaValidation());
    // hash = 1, R = G, s = 1 recover infinity: legacy returns an address, strict rejects it.
    byte[] input = new byte[128];
    input[31] = 1;
    input[63] = 27;
    byte[] r = BigIntegers.asUnsignedByteArray(
        32, ECKey.CURVE.getG().getAffineXCoord().toBigInteger());
    System.arraycopy(r, 0, input, 64, r.length);
    input[127] = 1;
    byte[] expected = strict ? new byte[0] : new DataWord(ByteArray.fromHexString(
        "41dcc703c0e500b653ca82273b7bfad8045d85a470")).getData();
    assertArrayEquals(expected, new PrecompiledContracts.ECRecover().execute(input).getRight());
  }
}
