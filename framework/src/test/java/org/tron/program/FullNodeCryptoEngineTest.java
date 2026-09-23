package org.tron.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.typesafe.config.ConfigUtil;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.exit.ExitManager;
import org.tron.common.log.LogService;
import org.tron.common.prometheus.Metrics;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.WitnessInitializer;

public class FullNodeCryptoEngineTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() {
    Args.clearParam();
  }

  @After
  public void tearDown() {
    Args.clearParam();
  }

  @Test
  public void testStartupWithoutCryptoEngine() throws Exception {
    assertStartup("");
  }

  @Test
  public void testStartupWithEckey() throws Exception {
    assertStartup("crypto.engine = eckey\n");
  }

  @Test
  public void testUnsupportedEngineBeforeDatabaseAndKeystore() throws Exception {
    for (String engine : new String[]{"sm2", "unknown", "null", "\"\"", "false"}) {
      File storage = new File(temporaryFolder.newFolder(), "legacy-database");
      File config = writeConfig("crypto.engine = " + engine + "\n"
          + "localwitness = []\nlocalwitnesskeystore = [\"legacy-sm2.json\"]\n"
          + "storage.properties = [{ name = account, path = "
          + ConfigUtil.quoteString(storage.toString()) + " }]\n");
      File output = new File(temporaryFolder.newFolder(), "chain");
      try (MockedStatic<ExitManager> exit = mockStatic(ExitManager.class);
           MockedStatic<WitnessInitializer> witness = mockStatic(WitnessInitializer.class);
           MockedStatic<KeystoreFactory> keystore = mockStatic(KeystoreFactory.class);
           MockedConstruction<TronApplicationContext> contexts =
               mockConstruction(TronApplicationContext.class)) {
        IllegalArgumentException exception = assertThrows(engine, IllegalArgumentException.class,
            () -> FullNode.main(new String[]{"-c", config.toString(), "-d", output.toString(),
                "-w", "--password", "test-password"}));
        assertTrue(exception.getMessage().contains("SM2/SM3 support has been removed"));
        assertTrue(contexts.constructed().isEmpty());
        witness.verifyNoInteractions();
        keystore.verifyNoInteractions();
        assertNull(Args.getStorageConfig());
        assertFalse(storage.exists());
        assertFalse(output.exists());

        // The legacy keystore-factory entry point must reject the same configuration.
        Args.clearParam();
        IllegalArgumentException keystoreException = assertThrows(IllegalArgumentException.class,
            () -> FullNode.main(new String[]{"-c", config.toString(), "--keystore-factory"}));
        assertTrue(keystoreException.getMessage().contains("SM2/SM3 support has been removed"));
        keystore.verifyNoInteractions();
        assertTrue(contexts.constructed().isEmpty());
        assertFalse(storage.exists());
      }
      Args.clearParam();
    }
  }

  private void assertStartup(String cryptoConfig) throws Exception {
    File config = writeConfig(cryptoConfig);
    File output = new File(temporaryFolder.newFolder(), "chain");
    Application application = mock(Application.class);
    // Exercise the real entry point and config loader, while isolating services and chain stores.
    try (MockedStatic<ExitManager> exit = mockStatic(ExitManager.class);
         MockedStatic<LogService> logs = mockStatic(LogService.class);
         MockedStatic<Metrics> metrics = mockStatic(Metrics.class);
         MockedStatic<ApplicationFactory> factory = mockStatic(ApplicationFactory.class);
         MockedConstruction<TronApplicationContext> contexts =
             mockConstruction(TronApplicationContext.class)) {
      factory.when(() -> ApplicationFactory.create(any())).thenReturn(application);
      FullNode.main(new String[]{"-c", config.toString(), "-d", output.toString()});
      assertEquals(1, contexts.constructed().size());
      verify(contexts.constructed().get(0)).refresh();
      verify(application).startup();
      verify(application).blockUntilShutdown();
    }
  }

  private File writeConfig(String cryptoConfig) throws Exception {
    File config = temporaryFolder.newFile();
    String contents = "include \"config-test.conf\"\n" + cryptoConfig;
    Files.write(config.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    return config;
  }
}
