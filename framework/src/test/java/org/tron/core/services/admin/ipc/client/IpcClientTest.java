package org.tron.core.services.admin.ipc.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Arrays;
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
  public void testListActivePeersDefaultsAndExplicitJsonKeepRpcRequestUnchanged()
      throws Exception {
    for (String command : Arrays.asList(
        "admin_listActivePeers", "admin_listActivePeers json")) {
      ExecResult execution = executeCommand(command,
          createActivePeersResponse("192.0.2.20:18888"));

      Assert.assertEquals(IpcClient.EXIT_SUCCESS, execution.exitCode);
      JsonNode request = OBJECT_MAPPER.readTree(execution.request);
      Assert.assertEquals("admin_listActivePeers", request.get("method").asText());
      Assert.assertEquals(0, request.get("params").size());
      Assert.assertTrue(execution.standardOutput,
          execution.standardOutput.contains("\"allCount\" : 1"));
      Assert.assertTrue(execution.standardOutput,
          execution.standardOutput.contains("\"remoteAddress\" : \"192.0.2.20:18888\""));
      Assert.assertFalse(execution.standardOutput,
          execution.standardOutput.contains(" | "));
      Assert.assertEquals("", execution.errorOutput);
    }
  }

  @Test
  public void testListActivePeersTextRendersTableWithoutChangingRpcRequest() throws Exception {
    ExecResult execution = executeCommand("admin_listActivePeers text",
        createActivePeersResponse("192.0.2.20:18888"));

    Assert.assertEquals(IpcClient.EXIT_SUCCESS, execution.exitCode);
    JsonNode request = OBJECT_MAPPER.readTree(execution.request);
    Assert.assertEquals("admin_listActivePeers", request.get("method").asText());
    Assert.assertEquals(0, request.get("params").size());
    Assert.assertTrue(execution.standardOutput,
        execution.standardOutput.contains("Peers: all=1, active=1, passive=0, valid=1"));
    for (String field : Arrays.asList(
        "remoteAddress", "connectSeconds", "averageLatencyMillis", "lastKnownBlockNum",
        "needSyncFromPeer", "needSyncFromUs", "syncToFetchSize", "syncToFetchSizePeekNum",
        "syncBlockRequestedSize", "remainNum", "syncChainRequestedMillis", "inactiveSeconds",
        "blockInProcess")) {
      Assert.assertTrue(field, execution.standardOutput.contains(field));
    }
    Assert.assertTrue(execution.standardOutput,
        execution.standardOutput.contains("192.0.2.20:18888"));
    Assert.assertTrue(execution.standardOutput,
        execution.standardOutput.contains("Units: Conn/Idle=s, Latency/Chain=ms"));
    Assert.assertTrue(execution.standardOutput,
        execution.standardOutput.contains(
            "Address          | Conn | Latency | Block    | Sync P/U | Fetch Size/Head | "
                + "Req/Remain | Chain | Idle | Proc"));
    Assert.assertTrue(execution.standardOutput,
        execution.standardOutput.contains(
            "192.0.2.20:18888 | 125  | 35      | 81234567 | N/N      | 3/81234560      | "
                + "2/7        | 450   | 2    | 1"));
    Assert.assertTrue(execution.standardOutput,
        execution.standardOutput.indexOf("Fields:")
            < execution.standardOutput.indexOf("Address          | Conn"));
    for (String line : execution.standardOutput.split("\\r?\\n", -1)) {
      Assert.assertTrue("Expected text output line to fit within 120 characters: " + line,
          line.length() <= 120);
    }
    Assert.assertFalse(execution.standardOutput,
        execution.standardOutput.contains("\"remoteAddress\""));
    Assert.assertEquals("", execution.errorOutput);
  }

  @Test
  public void testListActivePeersTextSanitizesTerminalControlCharacters() throws Exception {
    ExecResult execution = executeCommand("admin_listActivePeers text",
        createActivePeersResponse("peer\u001b[31m|address"));

    Assert.assertEquals(IpcClient.EXIT_SUCCESS, execution.exitCode);
    Assert.assertFalse(execution.standardOutput,
        execution.standardOutput.contains("\u001b"));
    Assert.assertTrue(execution.standardOutput,
        execution.standardOutput.contains("peer?[31m?address"));
  }

  @Test
  public void testListActivePeersRejectsUnsupportedOutputFormat() throws Exception {
    ExecResult execution = executeCommand("admin_listActivePeers yaml",
        createActivePeersResponse("192.0.2.20:18888"));

    Assert.assertEquals(IpcClient.EXIT_FAILURE, execution.exitCode);
    Assert.assertEquals("", execution.request);
    Assert.assertEquals("", execution.standardOutput);
    Assert.assertEquals("Invalid value for <format>; expected json or text"
        + System.lineSeparator(), execution.errorOutput);
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
          new IpcClient("unused").runExec(socket, "  admin_addPeer \"hello world\" \t"));
    } finally {
      System.setOut(originalOut);
      capturedOut.close();
    }

    JsonNode request = OBJECT_MAPPER.readTree(requestOutput.toString("UTF-8"));
    Assert.assertEquals("admin_addPeer", request.get("method").asText());
    Assert.assertEquals("hello world", request.get("params").get(0).asText());
    Assert.assertEquals(1, request.get("params").size());
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
    assertLocalExec("admin_addPeer \"sensitive-value", IpcClient.EXIT_FAILURE, "",
        "Invalid command syntax." + System.lineSeparator());
  }

  @Test
  public void testExecHelpAndExitSucceedWithoutSendingRequest() throws Exception {
    assertLocalExec("HELP ADMIN_ADDPEER", IpcClient.EXIT_SUCCESS,
        "usage: admin_addPeer <endpoint:string>" + System.lineSeparator(), "");
    assertLocalExec("QUIT", IpcClient.EXIT_SUCCESS, "", "");
  }

  @Test
  public void testExecInvalidCommandsFailWithoutSendingRequest() throws Exception {
    assertLocalExec(" \t ", IpcClient.EXIT_FAILURE, "",
        "No command specified for --exec." + System.lineSeparator());
    assertLocalExec("admin_addPeer", IpcClient.EXIT_FAILURE, "",
        "Invalid parameter, usage: admin_addPeer <endpoint:string>"
            + System.lineSeparator());
    assertLocalExec("unknown secret", IpcClient.EXIT_FAILURE,
        String.join(System.lineSeparator(), "Available commands:",
            "  admin_addPeer <endpoint:string>",
            "  admin_blockIp <ip:string>",
            "  admin_disconnectPeer <endpoint:string>",
            "  admin_listActivePeers [format:json|text]",
            "  admin_listBlockedIps",
            "  admin_removePeer <endpoint:string>",
            "  admin_unblockIp <ip:string>",
            "  help [command]", "  exit/quit", ""),
        "Invalid command." + System.lineSeparator());
  }

  @Test(timeout = 10_000)
  public void testExecLongUnknownCommandDoesNotEchoInputOrSendRequest() throws Exception {
    char[] command = new char[100_000];
    Arrays.fill(command, 'x');

    assertLocalExec(new String(command), IpcClient.EXIT_FAILURE,
        String.join(System.lineSeparator(), "Available commands:",
            "  admin_addPeer <endpoint:string>",
            "  admin_blockIp <ip:string>",
            "  admin_disconnectPeer <endpoint:string>",
            "  admin_listActivePeers [format:json|text]",
            "  admin_listBlockedIps",
            "  admin_removePeer <endpoint:string>",
            "  admin_unblockIp <ip:string>",
            "  help [command]", "  exit/quit", ""),
        "Invalid command." + System.lineSeparator());
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
          new IpcClient("unused").runExec(socket, "admin_addPeer 192.0.2.20:18888"));
    } finally {
      System.setErr(originalErr);
      capturedErr.close();
    }

    Assert.assertEquals("Error -32603: Internal error" + System.lineSeparator(),
        errorOutput.toString("UTF-8"));
  }

  @Test
  public void testExecRejectsInvalidResponses() throws Exception {
    String valid = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":true}";
    for (String response : new String[] {"{\"result\":true}",
        "{\"id\":1,\"result\":true}", "{\"jsonrpc\":\"2.0\",\"result\":true}",
        "{\"jsonrpc\":\"1.0\",\"id\":1,\"result\":true}",
        "{\"jsonrpc\":2.0,\"id\":1,\"result\":true}",
        "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":true,\"error\":null}",
        "{\"jsonrpc\":\"2.0\",\"id\":1}",
        "{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{}}",
        "{broken-sensitive-detail", "[]", valid + " garbage", valid + " " + valid}) {
      assertExecResponse(new IpcClient("unused"), 1, response, IpcClient.EXIT_FAILURE, "",
          "Invalid IPC response." + System.lineSeparator());
    }
  }

  @Test
  public void testExecRejectsMismatchedResponseIds() throws Exception {
    for (String id : new String[] {"2", "\"1\"", "null", "4294967297", "1.5"}) {
      String response = "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":true}";
      assertExecResponse(new IpcClient("unused"), 1, response, IpcClient.EXIT_FAILURE, "",
          "IPC response ID does not match request." + System.lineSeparator());
    }
  }

  @Test
  public void testExecAcceptsNullAndFalseResults() throws Exception {
    for (String value : new String[] {"null", "false"}) {
      String response = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":" + value + "}";
      assertExecResponse(new IpcClient("unused"), 1, response, IpcClient.EXIT_SUCCESS,
          value + System.lineSeparator(), "");
    }
  }

  @Test
  public void testExecUsesEachCommandsRequestId() throws Exception {
    IpcClient client = new IpcClient("unused");
    assertExecResponse(client, 1, "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":true}",
        IpcClient.EXIT_SUCCESS, "true" + System.lineSeparator(), "");
    String secondResponse = "{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":true}";
    assertExecResponse(client, 2, secondResponse,
        IpcClient.EXIT_SUCCESS, "true" + System.lineSeparator(), "");
    assertExecResponse(client, 3, secondResponse, IpcClient.EXIT_FAILURE, "",
        "IPC response ID does not match request." + System.lineSeparator());
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
          new IpcClient("unused").runExec(socket, "admin_addPeer 192.0.2.20:18888"));
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
          new IpcClient("unused").runExec(socket, "admin_addPeer 192.0.2.20:18888"));
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
        .thenReturn("", "help admin_addPeer", "admin_addPeer \"secret",
            "admin_addPeer", "admin_addPeer 'hello world'", "exit");
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
      Assert.assertEquals("admin_addPeer", request.get("method").asText());
      Assert.assertEquals(OBJECT_MAPPER.readTree("[\"hello world\"]"), request.get("params"));
      Assert.assertEquals(1, request.get("id").asInt());
      Assert.assertNull("Only the valid command should reach the server", requests.readLine());
      Mockito.verify(reader, Mockito.never()).printAbove("Disconnected from server.");
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }

    Assert.assertEquals("usage: admin_addPeer <endpoint:string>"
        + System.lineSeparator(), consoleOutput.toString("UTF-8"));
    Assert.assertEquals(String.join(System.lineSeparator(),
        "Invalid command syntax.", "Invalid command syntax.",
        "Invalid parameter, usage: admin_addPeer <endpoint:string>", ""),
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
      String responses = "\ninvalid-response\n"
          + "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"response\"}\n\n";
      serverConnection.getOutputStream().write(responses.getBytes(StandardCharsets.UTF_8));
      serverConnection.getOutputStream().flush();
      Mockito.verify(reader, Mockito.timeout(5_000)).printAbove("response");
      Mockito.verify(reader).printAbove("Invalid IPC response.");

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

  private void assertExecResponse(IpcClient client, int requestId, String response, int exitCode,
      String output, String error) throws Exception {
    Socket socket = Mockito.mock(Socket.class);
    ByteArrayOutputStream requestOutput = new ByteArrayOutputStream();
    Mockito.when(socket.getOutputStream()).thenReturn(requestOutput);
    Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(
        (response + "\n").getBytes(StandardCharsets.UTF_8)));
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
    ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
    try (PrintStream capturedOut = new PrintStream(consoleOutput, true, "UTF-8");
        PrintStream capturedErr = new PrintStream(errorOutput, true, "UTF-8")) {
      System.setOut(capturedOut);
      System.setErr(capturedErr);
      Assert.assertEquals(response, exitCode, client.runExec(socket, "admin_listBlockedIps"));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
    JsonNode request = OBJECT_MAPPER.readTree(requestOutput.toByteArray());
    Assert.assertEquals(requestId, request.get("id").asInt());
    Assert.assertEquals("admin_listBlockedIps", request.get("method").asText());
    Assert.assertEquals(response, output, consoleOutput.toString("UTF-8"));
    Assert.assertEquals(response, error, errorOutput.toString("UTF-8"));
    Mockito.verify(socket).setSoTimeout(30_000);
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

  private ExecResult executeCommand(String command, String response) throws Exception {
    Socket socket = Mockito.mock(Socket.class);
    ByteArrayOutputStream requestOutput = new ByteArrayOutputStream();
    Mockito.when(socket.getOutputStream()).thenReturn(requestOutput);
    Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(
        (response + "\n").getBytes(StandardCharsets.UTF_8)));

    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
    ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
    PrintStream capturedOut = new PrintStream(standardOutput, true, "UTF-8");
    PrintStream capturedErr = new PrintStream(errorOutput, true, "UTF-8");
    int exitCode;
    try {
      System.setOut(capturedOut);
      System.setErr(capturedErr);
      exitCode = new IpcClient("unused").runExec(socket, command);
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
      capturedOut.close();
      capturedErr.close();
    }
    return new ExecResult(exitCode, requestOutput.toString("UTF-8"),
        standardOutput.toString("UTF-8"), errorOutput.toString("UTF-8"));
  }

  private String createActivePeersResponse(String remoteAddress) throws Exception {
    ObjectNode response = OBJECT_MAPPER.createObjectNode();
    response.put("jsonrpc", "2.0");
    response.put("id", 1);
    ObjectNode result = response.putObject("result");
    result.put("allCount", 1);
    result.put("activeCount", 1);
    result.put("passiveCount", 0);
    result.put("validCount", 1);
    ArrayNode peers = result.putArray("peers");
    ObjectNode peer = peers.addObject();
    peer.put("remoteAddress", remoteAddress);
    peer.put("connectSeconds", 125);
    peer.put("averageLatencyMillis", 35);
    peer.put("lastKnownBlockNum", 81_234_567);
    peer.put("needSyncFromPeer", false);
    peer.put("needSyncFromUs", false);
    peer.put("syncToFetchSize", 3);
    peer.put("syncToFetchSizePeekNum", 81_234_560);
    peer.put("syncBlockRequestedSize", 2);
    peer.put("remainNum", 7);
    peer.put("syncChainRequestedMillis", 450);
    peer.put("inactiveSeconds", 2);
    peer.put("blockInProcess", 1);
    return OBJECT_MAPPER.writeValueAsString(response);
  }

  private static class ExecResult {

    private final int exitCode;
    private final String request;
    private final String standardOutput;
    private final String errorOutput;

    private ExecResult(int exitCode, String request, String standardOutput, String errorOutput) {
      this.exitCode = exitCode;
      this.request = request;
      this.standardOutput = standardOutput;
      this.errorOutput = errorOutput;
    }
  }

}
