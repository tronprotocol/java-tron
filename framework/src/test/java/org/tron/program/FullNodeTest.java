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
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.arch.Arch;
import org.tron.common.exit.ExitManager;
import org.tron.common.log.LogService;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.Metrics;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.CommandLineArguments;
import org.tron.core.exception.TronError;
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
    parameter.logbackPath = "node-logback.xml";
    Application application = Mockito.mock(Application.class);
    try (MockedStatic<ExitManager> exitManager = Mockito.mockStatic(ExitManager.class);
        MockedStatic<Arch> arch = Mockito.mockStatic(Arch.class);
        MockedStatic<Args> args = Mockito.mockStatic(Args.class);
        MockedStatic<CommonParameter> parameters = Mockito.mockStatic(CommonParameter.class);
        MockedStatic<LogService> logService = Mockito.mockStatic(LogService.class);
        MockedStatic<Metrics> metrics = Mockito.mockStatic(Metrics.class);
        MockedStatic<ApplicationFactory> factory = Mockito.mockStatic(ApplicationFactory.class);
        MockedConstruction<TronApplicationContext> contexts =
            Mockito.mockConstruction(TronApplicationContext.class);
        MockedConstruction<IpcClient> ipcClients = Mockito.mockConstruction(IpcClient.class)) {
      parameters.when(CommonParameter::getInstance).thenReturn(parameter);
      factory.when(() -> ApplicationFactory.create(Mockito.any())).thenReturn(application);

      FullNode.main(new String[] {"--log-config", "node-logback.xml"});

      exitManager.verify(ExitManager::initExceptionHandler);
      arch.verify(Arch::throwIfUnsupportedJavaVersion);
      args.verify(() -> Args.setParam(Mockito.argThat((CommandLineArguments parsed) ->
          "node-logback.xml".equals(parsed.getParameters().logbackPath)),
          Mockito.eq("config.conf")));
      logService.verify(() -> LogService.load("node-logback.xml"));
      metrics.verify(Metrics::init);
      Assert.assertEquals(1, contexts.constructed().size());
      Mockito.verify(contexts.constructed().get(0)).refresh();
      InOrder order = Mockito.inOrder(application);
      order.verify(application).startup();
      order.verify(application).blockUntilShutdown();
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
    assertArgumentError("Error: --attach requires a non-empty <socket-path>",
        "--attach", "");
    assertArgumentError("Error: --attach requires a non-empty <socket-path>",
        "--attach", " ");
    assertArgumentError("Error: --attach cannot be combined with: --config",
        "--attach", "missing.sock", "--config", "config.conf");
    assertArgumentError("Error: --exec requires --attach <socket-path>",
        "--exec", "help");
  }

  @Test(timeout = 60_000)
  public void testArgumentParsingErrorsDoNotPrintStackTraces() throws Exception {
    assertArgumentError("Error: Expected a value after parameter --attach", "--attach");
    assertArgumentError("Error: Expected a value after parameter --exec",
        "--attach", "missing.sock", "--exec");
    String invalidInteger =
        "Error: \"--rpc-thread\": couldn't convert \"not-a-number\" to an integer";
    assertArgumentError(invalidInteger,
        "--attach", "missing.sock", "--rpc-thread", "not-a-number");
    assertArgumentError(invalidInteger, "--rpc-thread", "not-a-number");
  }

  @Test(timeout = 60_000)
  public void testRemovedOptionsInAttachModeDoNotCreateNodeLogs() throws Exception {
    String migration = "--keystore-factory was removed." + System.lineSeparator()
        + "Use: java -jar Toolkit.jar keystore <new|import|list|update>";
    assertArgumentError(migration,
        "--attach", "missing.sock", "--keystore-factory");
    assertArgumentError(migration,
        "--attach", "missing.sock", "--keystore-factory", "--keystore-factory");
  }

  @Test
  public void testUnexpectedArgumentFailuresAreNotSuppressed() {
    for (Throwable failure : new Throwable[] {new IllegalStateException("unexpected failure"),
        new TronError("unexpected node error", TronError.ErrCode.JDK_VERSION)}) {
      try (MockedConstruction<CommandLineArguments> arguments = Mockito.mockConstruction(
          CommandLineArguments.class, (parsed, context) ->
              Mockito.when(parsed.isAttachMode()).thenThrow(failure))) {
        Assert.assertSame(failure,
            Assert.assertThrows(failure.getClass(), () -> FullNode.main(new String[0])));
        Assert.assertEquals(1, arguments.constructed().size());
      }
    }
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
          "--exec", "admin_addPeer 192.0.2.20:18888");
      Assert.assertEquals("admin_addPeer",
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

  private void assertArgumentError(String expectedError, String... args) throws Exception {
    String errorOutput = runClientWithoutNodeLogs(1, expectedError, "", args);
    // Keep inherited JVM diagnostics enabled, but exclude their startup notice from the CLI error.
    String javaToolOptions = System.getenv("JAVA_TOOL_OPTIONS");
    if (javaToolOptions != null) {
      String notice = "Picked up JAVA_TOOL_OPTIONS: " + javaToolOptions + System.lineSeparator();
      if (errorOutput.startsWith(notice)) {
        errorOutput = errorOutput.substring(notice.length());
      }
    }
    Assert.assertEquals(expectedError + System.lineSeparator(), errorOutput);
  }

  private String runClientWithoutNodeLogs(int expectedExitCode, String expectedOutput,
      String input, String... args) throws Exception {
    String classpath = System.getProperty("fullNode.runtimeClasspath");
    Assert.assertNotNull("The child JVM must use the production runtime classpath", classpath);
    Path directory = temporaryFolder.newFolder().toPath();
    File outputFile = directory.resolve("console.txt").toFile();
    File errorFile = directory.resolve("stderr.txt").toFile();
    List<String> command = new ArrayList<>();
    command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
    command.add("-cp");
    command.add(classpath);
    command.add(FullNode.class.getName());
    command.addAll(Arrays.asList(args));
    ProcessBuilder processBuilder = new ProcessBuilder(command).directory(directory.toFile())
        .redirectError(errorFile).redirectOutput(outputFile);
    // CI sets JAVA_TOOL_OPTIONS, whose JVM startup notice would pollute exact stderr checks.
    processBuilder.environment().remove("JAVA_TOOL_OPTIONS");
    Process process = processBuilder.start();
    try {
      process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      Assert.assertTrue("Attach subprocess did not exit", process.waitFor(20, TimeUnit.SECONDS));
      String stdout = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
      String stderr = new String(Files.readAllBytes(errorFile.toPath()), StandardCharsets.UTF_8);
      String output = stdout + stderr;
      Assert.assertEquals(output, expectedExitCode, process.exitValue());
      Assert.assertTrue(output, output.contains(expectedOutput));
      if (expectedExitCode != 0) {
        Assert.assertEquals("Errors must be written to stderr", "", stdout);
      }
      Assert.assertFalse("Attach created node log files: " + output,
          Files.exists(directory.resolve("logs")));
      Assert.assertFalse("Attach initialized the node data directory",
          Files.exists(directory.resolve("output-directory")));
      return stderr;
    } finally {
      process.destroyForcibly();
      Assert.assertTrue("Attach subprocess did not terminate",
          process.waitFor(5, TimeUnit.SECONDS));
    }
  }
}
