package org.tron.program;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.tron.common.arch.Arch;
import org.tron.common.exit.ExitManager;
import org.tron.common.log.LogService;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.CommandLineArguments;
import org.tron.core.services.admin.ipc.client.IpcClient;
import org.tron.core.services.jsonrpc.JsonRpcMapper;

public class FullNodeTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testAttachSkipsNodeInitialization() {
    try (MockedStatic<ExitManager> exitManager = Mockito.mockStatic(ExitManager.class);
        MockedStatic<Arch> arch = Mockito.mockStatic(Arch.class);
        MockedStatic<Args> args = Mockito.mockStatic(Args.class);
        MockedStatic<LogService> logService = Mockito.mockStatic(LogService.class);
        MockedConstruction<IpcClient> ipcClients = Mockito.mockConstruction(IpcClient.class,
            (client, context) -> Assert.assertEquals(Arrays.asList("/tmp/java-tron.sock"),
                context.arguments()))) {
      FullNode.main(new String[] {"--attach", "/tmp/java-tron.sock", "--exec", "help"});

      Assert.assertEquals(1, ipcClients.constructed().size());
      Mockito.verify(ipcClients.constructed().get(0)).start("help");
      exitManager.verifyNoInteractions();
      arch.verifyNoInteractions();
      args.verifyNoInteractions();
      logService.verifyNoInteractions();
    }
  }

  @Test
  public void testNodeModeRetainsInitialization() {
    CommonParameter parameter = new CommonParameter();
    parameter.keystoreFactory = true;
    parameter.logbackPath = "node-logback.xml";
    try (MockedStatic<ExitManager> exitManager = Mockito.mockStatic(ExitManager.class);
        MockedStatic<Arch> arch = Mockito.mockStatic(Arch.class);
        MockedStatic<Args> args = Mockito.mockStatic(Args.class);
        MockedStatic<CommonParameter> parameters = Mockito.mockStatic(CommonParameter.class);
        MockedStatic<LogService> logService = Mockito.mockStatic(LogService.class);
        MockedStatic<KeystoreFactory> keystore = Mockito.mockStatic(KeystoreFactory.class);
        MockedConstruction<IpcClient> ipcClients = Mockito.mockConstruction(IpcClient.class)) {
      parameters.when(CommonParameter::getInstance).thenReturn(parameter);

      FullNode.main(new String[] {"--keystore-factory"});

      exitManager.verify(ExitManager::initExceptionHandler);
      arch.verify(Arch::throwIfUnsupportedJavaVersion);
      args.verify(() -> Args.setParam(Mockito.argThat((CommandLineArguments parsed) ->
          parsed.getParameters().keystoreFactory), Mockito.eq("config.conf")));
      logService.verify(() -> LogService.load("node-logback.xml"));
      keystore.verify(KeystoreFactory::start);
      Assert.assertTrue(ipcClients.constructed().isEmpty());
    }
  }

  @Test(timeout = 30_000)
  public void testMissingAttachSocketDoesNotCreateNodeLogs() throws Exception {
    runClientWithoutNodeLogs(1, "IPC socket file does not exist: missing.sock", "",
        "--attach", "missing.sock", "--exec", "help");
  }

  @Test(timeout = 60_000)
  public void testInvalidAttachOptionsDoNotCreateNodeLogs() throws Exception {
    runClientWithoutNodeLogs(1, "--attach requires a non-empty <socket-path>", "",
        "--attach", "");
    runClientWithoutNodeLogs(1, "--attach requires a non-empty <socket-path>", "",
        "--attach", " ");
    runClientWithoutNodeLogs(1, "--attach cannot be combined with: --config", "",
        "--attach", "missing.sock", "--config", "config.conf");
    runClientWithoutNodeLogs(1, "--exec requires --attach <socket-path>", "",
        "--exec", "help");
  }

  @Test(timeout = 30_000)
  public void testExecResponseDoesNotCreateNodeLogs() throws Exception {
    Assume.assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
    Assume.assumeTrue(AFUNIXSocket.isSupported());
    // Keep the socket path below the macOS sun_path limit, even with a long JVM temp directory.
    Path socketDirectory = Files.createTempDirectory(Paths.get("/tmp"), "attach-test-");
    Path socketFile = socketDirectory.resolve("node.sock");
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (AFUNIXServerSocket server = AFUNIXServerSocket.bindOn(
        AFUNIXSocketAddress.of(socketFile.toFile()))) {
      server.setSoTimeout(10_000);
      Future<String> request = executor.submit(() -> {
        try (AFUNIXSocket connection = server.accept();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
          connection.setSoTimeout(10_000);
          String line = reader.readLine();
          connection.getOutputStream().write(
              "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"one:two\"}\n"
                  .getBytes(StandardCharsets.UTF_8));
          connection.getOutputStream().flush();
          return line;
        }
      });

      runClientWithoutNodeLogs(0, "one:two", "", "--attach", socketFile.toString(),
          "--exec", "admin_example one two");
      Assert.assertEquals("admin_example",
          JsonRpcMapper.create().readTree(request.get(5, TimeUnit.SECONDS)).get("method").asText());
    } finally {
      executor.shutdownNow();
      Files.deleteIfExists(socketFile);
      Files.deleteIfExists(socketDirectory);
    }
  }

  @Test(timeout = 30_000)
  public void testInteractiveExitDoesNotCreateNodeLogs() throws Exception {
    Assume.assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
    Assume.assumeTrue(AFUNIXSocket.isSupported());
    Path socketDirectory = Files.createTempDirectory(Paths.get("/tmp"), "attach-test-");
    Path socketFile = socketDirectory.resolve("node.sock");
    try (AFUNIXServerSocket server = AFUNIXServerSocket.bindOn(
        AFUNIXSocketAddress.of(socketFile.toFile()))) {
      runClientWithoutNodeLogs(0, "Welcome to the java-tron admin console.", "exit\n",
          "--attach", socketFile.toString());
    } finally {
      Files.deleteIfExists(socketFile);
      Files.deleteIfExists(socketDirectory);
    }
  }

  private void runClientWithoutNodeLogs(int expectedExitCode, String expectedOutput,
      String input, String... args) throws Exception {
    String classpath = System.getProperty("fullNode.runtimeClasspath");
    Assert.assertNotNull("The child JVM must use the production runtime classpath", classpath);
    Path directory = temporaryFolder.newFolder().toPath();
    File outputFile = directory.resolve("console.txt").toFile();
    List<String> command = new ArrayList<>();
    command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
    command.add("-cp");
    command.add(classpath);
    command.add(FullNode.class.getName());
    command.addAll(Arrays.asList(args));
    Process process = new ProcessBuilder(command).directory(directory.toFile())
        .redirectErrorStream(true).redirectOutput(outputFile).start();
    try {
      process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      Assert.assertTrue("Attach subprocess did not exit", process.waitFor(20, TimeUnit.SECONDS));
      String output = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
      Assert.assertEquals(output, expectedExitCode, process.exitValue());
      Assert.assertTrue(output, output.contains(expectedOutput));
      Assert.assertFalse("Attach created node log files: " + output,
          Files.exists(directory.resolve("logs")));
      Assert.assertFalse("Attach initialized the node data directory",
          Files.exists(directory.resolve("output-directory")));
    } finally {
      process.destroyForcibly();
      Assert.assertTrue("Attach subprocess did not terminate",
          process.waitFor(5, TimeUnit.SECONDS));
    }
  }
}
