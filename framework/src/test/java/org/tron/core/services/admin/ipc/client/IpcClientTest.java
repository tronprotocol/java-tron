package org.tron.core.services.admin.ipc.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jline.reader.LineReader;
import org.jline.reader.SyntaxError;
import org.jline.reader.UserInterruptException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class IpcClientTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void testClientDoesNotDeclareLogger() {
    try {
      IpcClient.class.getDeclaredField("logger");
      Assert.fail("IPC client must not initialize the node logging system");
    } catch (NoSuchFieldException expected) {
      // No logger field means loading IpcClient cannot initialize SLF4J through this class.
    }
  }

  @Test
  public void testExecSendsCommandAndPrintsFormattedResult() throws Exception {
    Socket socket = Mockito.mock(Socket.class);
    ByteArrayOutputStream requestOutput = new ByteArrayOutputStream();
    Mockito.when(socket.getOutputStream()).thenReturn(requestOutput);
    Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(
        "\n{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"hello world:b\"}\n"
            .getBytes(StandardCharsets.UTF_8)));

    PrintStream originalOut = System.out;
    ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
    PrintStream capturedOut = new PrintStream(consoleOutput, true, "UTF-8");
    try {
      System.setOut(capturedOut);
      Assert.assertEquals(IpcClient.EXIT_SUCCESS,
          new IpcClient("unused").runExec(socket, "  admin_example \"hello world\" b \t"));
    } finally {
      System.setOut(originalOut);
      capturedOut.close();
    }

    JsonNode request = OBJECT_MAPPER.readTree(requestOutput.toString("UTF-8"));
    Assert.assertEquals("admin_example", request.get("method").asText());
    Assert.assertEquals("hello world", request.get("params").get(0).asText());
    Assert.assertEquals("b", request.get("params").get(1).asText());
    Assert.assertEquals("hello world:b" + System.lineSeparator(),
        consoleOutput.toString("UTF-8"));
  }

  @Test
  public void testWelcomeShowsConnectionAndUsageHint() throws Exception {
    Path temporaryDirectory = Files.createTempDirectory("ipc-welcome-test-");
    File socketFile = temporaryDirectory.resolve("java-tron.1234.sock").toFile();
    PrintStream originalOut = System.out;
    ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
    PrintStream capturedOut = new PrintStream(consoleOutput, true, "UTF-8");
    try {
      System.setOut(capturedOut);
      new IpcClient(socketFile.getPath()).printWelcome(socketFile);
    } finally {
      System.setOut(originalOut);
      capturedOut.close();
      Files.deleteIfExists(temporaryDirectory);
    }

    String welcome = consoleOutput.toString("UTF-8");
    Assert.assertTrue(welcome, welcome.contains("Welcome to the java-tron admin console."));
    Assert.assertTrue(welcome, welcome.contains("IPC endpoint: " + socketFile.getAbsolutePath()));
    Assert.assertFalse(welcome, welcome.contains("History:"));
    Assert.assertTrue(welcome, welcome.contains("Type \"help\" for available commands"));
  }

  @Test
  public void testExecWithInvalidSyntaxDoesNotExposeCommand() throws Exception {
    assertLocalExec("admin_example \"sensitive-value", IpcClient.EXIT_FAILURE, "",
        "Invalid command syntax." + System.lineSeparator());
  }

  @Test
  public void testExecHelpAndExitSucceedWithoutSendingRequest() throws Exception {
    assertLocalExec("HELP ADMIN_EXAMPLE", IpcClient.EXIT_SUCCESS,
        "usage: admin_example <param1:string> <param2:string>" + System.lineSeparator(), "");
    assertLocalExec("QUIT", IpcClient.EXIT_SUCCESS, "", "");
  }

  @Test
  public void testExecInvalidCommandsFailWithoutSendingRequest() throws Exception {
    assertLocalExec(" \t ", IpcClient.EXIT_FAILURE, "",
        "No command specified for --exec." + System.lineSeparator());
    assertLocalExec("admin_example missing", IpcClient.EXIT_FAILURE, "",
        "Invalid parameter, usage: admin_example <param1:string> <param2:string>"
            + System.lineSeparator());
    assertLocalExec("unknown secret", IpcClient.EXIT_FAILURE,
        String.join(System.lineSeparator(), "Available commands:",
            "  admin_example <param1:string> <param2:string>",
            "  help [command]", "  exit/quit", ""),
        "Invalid cmd: unknown" + System.lineSeparator());
  }

  @Test
  public void testMissingSocketFilePrintsConsoleError() throws Exception {
    Path temporaryDirectory = Files.createTempDirectory("ipc-client-test-");
    Path missingSocket = temporaryDirectory.resolve("missing.sock");
    PrintStream originalErr = System.err;
    ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
    PrintStream capturedErr = new PrintStream(errorOutput, true, "UTF-8");
    try {
      System.setErr(capturedErr);
      Assert.assertEquals(IpcClient.EXIT_FAILURE,
          new IpcClient(missingSocket.toString()).run());
    } finally {
      System.setErr(originalErr);
      capturedErr.close();
      Files.deleteIfExists(temporaryDirectory);
    }

    Assert.assertEquals("Error: IPC socket file does not exist: missing.sock"
            + System.lineSeparator(),
        errorOutput.toString("UTF-8"));
  }

  @Test
  public void testExecReturnsFailureForRpcError() throws Exception {
    Socket socket = Mockito.mock(Socket.class);
    Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(
        ("{\"jsonrpc\":\"2.0\",\"id\":1,"
            + "\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}\n")
            .getBytes(StandardCharsets.UTF_8)));

    PrintStream originalErr = System.err;
    ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
    PrintStream capturedErr = new PrintStream(errorOutput, true, "UTF-8");
    try {
      System.setErr(capturedErr);
      Assert.assertEquals(IpcClient.EXIT_FAILURE,
          new IpcClient("unused").runExec(socket, "admin_example a b"));
    } finally {
      System.setErr(originalErr);
      capturedErr.close();
    }

    Assert.assertEquals("Error -32603: Internal error" + System.lineSeparator(),
        errorOutput.toString("UTF-8"));
  }

  @Test
  public void testExecReturnsFailureWhenServerDisconnects() throws Exception {
    Socket socket = Mockito.mock(Socket.class);
    Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    PrintStream originalErr = System.err;
    ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
    PrintStream capturedErr = new PrintStream(errorOutput, true, "UTF-8");
    try {
      System.setErr(capturedErr);
      Assert.assertEquals(IpcClient.EXIT_FAILURE,
          new IpcClient("unused").runExec(socket, "admin_example a b"));
    } finally {
      System.setErr(originalErr);
      capturedErr.close();
    }

    Assert.assertEquals(
        "Disconnected from server before receiving a response." + System.lineSeparator(),
        errorOutput.toString("UTF-8"));
  }

  @Test
  public void testExecTimesOutWaitingForResponse() throws Exception {
    Socket socket = Mockito.mock(Socket.class);
    Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    Mockito.when(socket.getInputStream()).thenReturn(new InputStream() {
      @Override
      public int read() throws IOException {
        throw new SocketTimeoutException("timed out");
      }
    });

    PrintStream originalErr = System.err;
    ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
    PrintStream capturedErr = new PrintStream(errorOutput, true, "UTF-8");
    try {
      System.setErr(capturedErr);
      Assert.assertEquals(IpcClient.EXIT_FAILURE,
          new IpcClient("unused").runExec(socket, "admin_example a b"));
    } finally {
      System.setErr(originalErr);
      capturedErr.close();
    }

    Mockito.verify(socket).setSoTimeout(30_000);
    Assert.assertEquals("Timed out waiting for IPC response." + System.lineSeparator(),
        errorOutput.toString("UTF-8"));
  }

  @Test(timeout = 10_000)
  public void testSessionContinuesAfterHelpAndInvalidInput() throws Exception {
    LineReader reader = Mockito.mock(LineReader.class);
    Mockito.when(reader.readLine("> "))
        .thenThrow(new SyntaxError(0, 0, "sensitive terminal input"))
        .thenReturn("", "help admin_example", "admin_example \"secret",
            "admin_example missing", "admin_example 'hello world' b", "exit");
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
    ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
    try (PrintStream capturedOut = new PrintStream(consoleOutput, true, "UTF-8");
        PrintStream capturedErr = new PrintStream(errorOutput, true, "UTF-8");
        ServerSocket serverSocket = new ServerSocket(0);
        Socket clientSocket = new Socket("127.0.0.1", serverSocket.getLocalPort());
        Socket serverConnection = serverSocket.accept()) {
      System.setOut(capturedOut);
      System.setErr(capturedErr);
      new IpcClient("unused").runSession(clientSocket, reader);

      Assert.assertFalse("The enclosing run() must own the socket", clientSocket.isClosed());
      clientSocket.shutdownOutput();
      serverConnection.setSoTimeout(1_000);
      BufferedReader requests = new BufferedReader(
          new InputStreamReader(serverConnection.getInputStream(), StandardCharsets.UTF_8));
      JsonNode request = OBJECT_MAPPER.readTree(requests.readLine());
      Assert.assertEquals("admin_example", request.get("method").asText());
      Assert.assertEquals(OBJECT_MAPPER.readTree("[\"hello world\",\"b\"]"), request.get("params"));
      Assert.assertEquals(1, request.get("id").asInt());
      Assert.assertNull("Only the valid command should reach the server", requests.readLine());
      Mockito.verify(reader, Mockito.never()).printAbove("Disconnected from server.");
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }

    Assert.assertEquals("usage: admin_example <param1:string> <param2:string>"
        + System.lineSeparator(), consoleOutput.toString("UTF-8"));
    Assert.assertEquals(String.join(System.lineSeparator(),
        "Invalid command syntax.", "Invalid command syntax.",
        "Invalid parameter, usage: admin_example <param1:string> <param2:string>", ""),
        errorOutput.toString("UTF-8"));
  }

  @Test(timeout = 10_000)
  public void testSessionPrintsResponseAndExitsWhenServerDisconnects() throws Exception {
    CountDownLatch inputStarted = new CountDownLatch(1);
    CountDownLatch waitForInterrupt = new CountDownLatch(1);
    LineReader reader = Mockito.mock(LineReader.class);
    Mockito.when(reader.readLine("> ")).thenAnswer(invocation -> {
      inputStarted.countDown();
      try {
        waitForInterrupt.await();
        return "";
      } catch (InterruptedException e) {
        throw new UserInterruptException("");
      }
    });

    try (ServerSocket serverSocket = new ServerSocket(0);
        Socket clientSocket = new Socket("127.0.0.1", serverSocket.getLocalPort());
        Socket serverConnection = serverSocket.accept()) {
      IpcClient client = new IpcClient("unused");
      AtomicReference<Throwable> failure = new AtomicReference<>();
      Thread sessionThread = new Thread(() -> {
        try {
          client.runSession(clientSocket, reader);
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      }, "ipc-client-test-session");
      sessionThread.setDaemon(true);
      sessionThread.start();

      Assert.assertTrue("IPC client did not start reading terminal input",
          inputStarted.await(5, TimeUnit.SECONDS));
      serverConnection.getOutputStream().write("\nresponse\n\n".getBytes(StandardCharsets.UTF_8));
      serverConnection.getOutputStream().flush();
      Mockito.verify(reader, Mockito.timeout(5_000)).printAbove("response");

      serverConnection.close();
      sessionThread.join(5_000);

      Assert.assertFalse("IPC client did not exit after server disconnected",
          sessionThread.isAlive());
      Assert.assertNull("IPC client session failed", failure.get());
      Mockito.verify(reader, Mockito.never()).printAbove("null");
      Mockito.verify(reader, Mockito.never()).printAbove("");
      Mockito.verify(reader).printAbove("Disconnected from server.");
    }
  }

  @Test(timeout = 10_000)
  public void testSessionExitDoesNotInterruptInputThread() throws Exception {
    LineReader reader = Mockito.mock(LineReader.class);
    Mockito.when(reader.readLine("> ")).thenReturn("exit");

    try (ServerSocket serverSocket = new ServerSocket(0);
        Socket clientSocket = new Socket("127.0.0.1", serverSocket.getLocalPort());
        Socket serverConnection = serverSocket.accept()) {
      IpcClient client = new IpcClient("unused");
      AtomicReference<Throwable> failure = new AtomicReference<>();
      AtomicBoolean interrupted = new AtomicBoolean(true);
      Thread sessionThread = new Thread(() -> {
        try {
          client.runSession(clientSocket, reader);
          interrupted.set(Thread.currentThread().isInterrupted());
        } catch (Throwable throwable) {
          failure.set(throwable);
        }
      }, "ipc-client-clean-exit-test-session");
      sessionThread.setDaemon(true);
      sessionThread.start();
      sessionThread.join(5_000);

      Assert.assertFalse("IPC client did not exit after the exit command", sessionThread.isAlive());
      Assert.assertNull("IPC client session failed", failure.get());
      Assert.assertFalse("Clean IPC client exit left the thread interrupted", interrupted.get());
      Mockito.verify(reader, Mockito.never()).printAbove("Disconnected from server.");
    }
  }

  @Test(timeout = 10_000)
  public void testSessionDoesNotSwallowUnexpectedRuntimeException() throws Exception {
    LineReader reader = Mockito.mock(LineReader.class);
    IllegalStateException expected = new IllegalStateException("unexpected failure");
    Mockito.when(reader.readLine("> ")).thenThrow(expected);

    try (ServerSocket serverSocket = new ServerSocket(0);
        Socket clientSocket = new Socket("127.0.0.1", serverSocket.getLocalPort());
        Socket serverConnection = serverSocket.accept()) {
      try {
        new IpcClient("unused").runSession(clientSocket, reader);
        Assert.fail("Expected the unexpected runtime exception to propagate");
      } catch (IllegalStateException e) {
        Assert.assertSame(expected, e);
      }
    }
  }

  private void assertLocalExec(String input, int exitCode, String output, String error)
      throws Exception {
    Socket socket = Mockito.mock(Socket.class);
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
    ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
    try (PrintStream capturedOut = new PrintStream(consoleOutput, true, "UTF-8");
        PrintStream capturedErr = new PrintStream(errorOutput, true, "UTF-8")) {
      System.setOut(capturedOut);
      System.setErr(capturedErr);
      Assert.assertEquals(exitCode, new IpcClient("unused").runExec(socket, input));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
    Assert.assertEquals(output, consoleOutput.toString("UTF-8"));
    Assert.assertEquals(error, errorOutput.toString("UTF-8"));
    Mockito.verifyNoInteractions(socket);
  }
}
