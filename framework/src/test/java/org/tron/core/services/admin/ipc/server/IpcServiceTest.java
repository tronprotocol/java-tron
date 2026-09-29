package org.tron.core.services.admin.ipc.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.exception.TronError;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.admin.AdminJsonRpcImpl;

public class IpcServiceTest {

  private int originalMaxMessageSize;
  private long originalJsonRpcMaxMessageSize;

  @Before
  public void setUp() {
    originalMaxMessageSize = Args.getInstance().maxMessageSize;
    originalJsonRpcMaxMessageSize = Args.getInstance().jsonRpcMaxMessageSize;
    Args.getInstance().maxMessageSize = 4 * 1024 * 1024;
    Args.getInstance().jsonRpcMaxMessageSize = 4 * 1024 * 1024;
  }

  @After
  public void tearDown() {
    Args.getInstance().maxMessageSize = originalMaxMessageSize;
    Args.getInstance().jsonRpcMaxMessageSize = originalJsonRpcMaxMessageSize;
  }

  @Test
  public void testServiceIsNotRunningBeforeStart() throws Exception {
    Assert.assertFalse(isRunning(newIpcService()));
  }

  @Test(timeout = 10_000)
  public void testRequestSizeUsesJsonRpcConfiguration() throws Exception {
    assumePosixFileSystem();
    CommonParameter parameter = Args.getInstance();
    for (long limit : new long[] {0, (long) Integer.MAX_VALUE + 1}) {
      parameter.jsonRpcMaxMessageSize = limit;
      parameter.maxMessageSize = limit == 0 ? 4 * 1024 * 1024 : 0;
      String originalOutputDirectory = parameter.outputDirectory;
      String originalSocketDirectory = parameter.ipcSocketDirectory;
      Path outputDirectory = Files.createTempDirectory(Paths.get("/tmp"), "ipc-limit-");
      AdminJsonRpc adminJsonRpc = Mockito.mock(AdminJsonRpc.class);
      IpcService service = new IpcService(adminJsonRpc);
      boolean started = false;
      Path socketFile = null;
      try {
        parameter.outputDirectory = outputDirectory.toString();
        parameter.ipcSocketDirectory = "";
        service.innerStart();
        started = true;
        socketFile = resolveSocketFilePath(parameter, getPid(service));

        try (AFUNIXSocket client = AFUNIXSocket.newInstance()) {
          client.connect(AFUNIXSocketAddress.of(socketFile.toFile()));
          client.setSoTimeout(2_000);
          if (limit == 0) {
            client.getOutputStream().write('{');
            client.getOutputStream().flush();
            Assert.assertEquals("A zero limit must close the connection on its first byte",
                -1, client.getInputStream().read());
            Mockito.verifyNoInteractions(adminJsonRpc);
          } else {
            Mockito.when(adminJsonRpc.adminExample("a", "b")).thenReturn("a:b");
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            assertSuccessfulResponse(sendRequest(writer, reader, 1), 1);
            Mockito.verify(adminJsonRpc).adminExample("a", "b");
          }
        }
      } finally {
        parameter.ipcSocketDirectory = originalSocketDirectory;
        cleanupIpcService(service, started, parameter, originalOutputDirectory, socketFile,
            outputDirectory);
      }
    }
  }

  @Test(timeout = 10_000)
  public void testSocketFileUsesOwnerOnlyPermissions() throws Exception {
    assumePosixFileSystem();
    CommonParameter parameter = Args.getInstance();
    String originalOutputDirectory = parameter.outputDirectory;
    Path outputDirectory = Files.createTempDirectory(Paths.get("/tmp"), "ipc-permission-test-");
    IpcService service = new IpcService(
        new AdminJsonRpcImpl());
    boolean started = false;
    Path socketFile = null;
    try {
      parameter.outputDirectory = outputDirectory.toString();
      Assert.assertTrue(service.start().get());
      started = true;

      socketFile = resolveSocketFilePath(parameter, getPid(service));
      Assert.assertEquals(
          EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
          Files.getPosixFilePermissions(socketFile));
    } finally {
      cleanupIpcService(service, started, parameter, originalOutputDirectory, socketFile,
          outputDirectory);
    }
  }

  @Test(timeout = 10_000)
  public void testRejectedStartupAndStopPreserveExistingPaths() throws Exception {
    assumePosixFileSystem();
    CommonParameter parameter = Args.getInstance();
    String originalSocketDirectory = parameter.ipcSocketDirectory;
    Path root = Files.createTempDirectory(Paths.get("/tmp"), "ipc-existing-");
    Path directory = root.resolve("ipc");
    Path target = Files.createDirectory(root.resolve("target"));
    Path unrelated = directory.resolve("operator-note.txt");
    byte[] contents = "keep me".getBytes(StandardCharsets.UTF_8);
    try {
      parameter.ipcSocketDirectory = root.toString();
      for (int kind = 0; kind < 4; kind++) {
        IpcService service = newIpcService();
        Path socketFile = resolveSocketFilePath(parameter, getPid(service));
        if (kind == 0 || kind == 1) {
          Files.createDirectory(directory);
          if (kind == 1) {
            Files.write(socketFile, contents);
            Files.write(unrelated, contents);
          }
        } else if (kind == 2) {
          Files.write(directory, contents);
        } else {
          Files.createSymbolicLink(directory, target);
        }
        try {
          assertStartupRejectsExistingDirectory(service);
          service.innerStop();

          Assert.assertTrue(Files.exists(directory, LinkOption.NOFOLLOW_LINKS));
          if (kind == 1) {
            Assert.assertArrayEquals(contents, Files.readAllBytes(socketFile));
            Assert.assertArrayEquals(contents, Files.readAllBytes(unrelated));
          } else if (kind == 2) {
            Assert.assertArrayEquals(contents, Files.readAllBytes(directory));
          } else if (kind == 3) {
            Assert.assertEquals(target, Files.readSymbolicLink(directory));
          }
        } finally {
          service.innerStop();
          if (kind == 1) {
            Files.deleteIfExists(socketFile);
            Files.deleteIfExists(unrelated);
          }
          Files.deleteIfExists(directory);
        }
      }
    } finally {
      parameter.ipcSocketDirectory = originalSocketDirectory;
      Files.deleteIfExists(target);
      Files.deleteIfExists(root);
    }
  }

  @Test(timeout = 15_000)
  public void testSecondNodeRejectionPreservesFirstNodeEndpoint() throws Exception {
    assumePosixFileSystem();
    CommonParameter parameter = Args.getInstance();
    String originalSocketDirectory = parameter.ipcSocketDirectory;
    Path root = Files.createTempDirectory(Paths.get("/tmp"), "ipc-two-nodes-");
    IpcService first = newIpcService();
    IpcService second = newIpcService();
    Path socketFile = null;
    try {
      parameter.ipcSocketDirectory = root.toString();
      first.innerStart();
      String firstPid = getPid(first);
      socketFile = resolveSocketFilePath(parameter, firstPid);
      assertNewConnectionSucceeds(socketFile);

      // Model another JVM's PID while exercising the real service and Unix socket.
      RuntimeMXBean secondRuntime = Mockito.mock(RuntimeMXBean.class);
      Mockito.when(secondRuntime.getName()).thenReturn(firstPid + "0@localhost");
      try (MockedStatic<ManagementFactory> management = Mockito.mockStatic(
          ManagementFactory.class, Mockito.CALLS_REAL_METHODS)) {
        management.when(ManagementFactory::getRuntimeMXBean).thenReturn(secondRuntime);
        Assert.assertNotEquals(firstPid, getPid(second));
        assertStartupRejectsExistingDirectory(second);
      }
      assertNewConnectionSucceeds(socketFile);
      second.innerStop();
      assertNewConnectionSucceeds(socketFile);
      first.innerStop();
      Assert.assertFalse(Files.exists(socketFile));
      Assert.assertFalse(Files.exists(socketFile.getParent()));
    } finally {
      parameter.ipcSocketDirectory = originalSocketDirectory;
      try {
        second.innerStop();
      } finally {
        first.innerStop();
        if (socketFile != null) {
          Files.deleteIfExists(socketFile);
          Files.deleteIfExists(socketFile.getParent());
        }
        Files.deleteIfExists(root);
      }
    }
  }

  @Test(timeout = 10_000)
  public void testBindFailureCleansOwnedDirectoryAndReleasesOwnership() throws Exception {
    assumePosixFileSystem();
    CommonParameter parameter = Args.getInstance();
    String originalSocketDirectory = parameter.ipcSocketDirectory;
    Path root = Files.createTempDirectory(Paths.get("/tmp"), "ipc-bind-fail-");
    Path directory = root.resolve("ipc");
    IpcService service = newIpcService();
    try {
      parameter.ipcSocketDirectory = root.toString();
      try (MockedStatic<AFUNIXServerSocket> sockets = Mockito.mockStatic(
          AFUNIXServerSocket.class)) {
        sockets.when(() -> AFUNIXServerSocket.bindOn(Mockito.any(AFUNIXSocketAddress.class)))
            .thenThrow(new IOException("bind failed"));
        try {
          service.innerStart();
          Assert.fail("Expected bind failure");
        } catch (IOException e) {
          Assert.assertEquals("bind failed", e.getMessage());
        }
      }
      Assert.assertFalse(Files.exists(directory));
      Files.createDirectory(directory);
      service.innerStop();
      Assert.assertTrue("Stop must not remove a replacement directory",
          Files.isDirectory(directory));
    } finally {
      parameter.ipcSocketDirectory = originalSocketDirectory;
      service.innerStop();
      Files.deleteIfExists(directory);
      Files.deleteIfExists(root);
    }
  }

  @Test(timeout = 10_000)
  public void testRepeatedStopPreservesReplacementPaths() throws Exception {
    assumePosixFileSystem();
    CommonParameter parameter = Args.getInstance();
    String originalSocketDirectory = parameter.ipcSocketDirectory;
    Path root = Files.createTempDirectory(Paths.get("/tmp"), "ipc-repeat-stop-");
    IpcService service = newIpcService();
    Path socketFile = null;
    try {
      parameter.ipcSocketDirectory = root.toString();
      service.innerStart();
      socketFile = resolveSocketFilePath(parameter, getPid(service));
      service.innerStop();
      Assert.assertFalse(Files.exists(socketFile.getParent()));
      Files.createDirectory(socketFile.getParent());
      byte[] contents = "replacement".getBytes(StandardCharsets.UTF_8);
      Files.write(socketFile, contents);

      service.innerStop();

      Assert.assertArrayEquals(contents, Files.readAllBytes(socketFile));
    } finally {
      parameter.ipcSocketDirectory = originalSocketDirectory;
      service.innerStop();
      if (socketFile != null) {
        Files.deleteIfExists(socketFile);
        Files.deleteIfExists(socketFile.getParent());
      }
      Files.deleteIfExists(root);
    }
  }

  @Test(timeout = 10_000)
  public void testInnerStartCleansSocketWhenPermissionUpdateFails() throws Exception {
    assumePosixFileSystem();
    CommonParameter parameter = Args.getInstance();
    String originalOutputDirectory = parameter.outputDirectory;
    String originalSocketDirectory = parameter.ipcSocketDirectory;
    Path socketDirectory = Files.createTempDirectory(Paths.get("/tmp"), "ipc-perm-fail-");
    IpcService service = newIpcService();
    Path socketFile = null;
    try {
      parameter.outputDirectory = socketDirectory.toString();
      parameter.ipcSocketDirectory = "";
      socketFile = resolveSocketFilePath(parameter, getPid(service));
      Set<PosixFilePermission> permissions = EnumSet.of(PosixFilePermission.OWNER_READ,
          PosixFilePermission.OWNER_WRITE);
      try (MockedStatic<Files> files = Mockito.mockStatic(Files.class,
          Mockito.CALLS_REAL_METHODS)) {
        Path expectedSocketFile = socketFile;
        files.when(() -> Files.setPosixFilePermissions(expectedSocketFile, permissions))
            .thenThrow(new IOException("permission update failed"));

        try {
          service.innerStart();
          Assert.fail("Expected the permission update failure to be preserved");
        } catch (IOException e) {
          Assert.assertEquals("permission update failed", e.getMessage());
        }
      }

      Assert.assertFalse(Files.exists(socketFile));
    } finally {
      parameter.outputDirectory = originalOutputDirectory;
      parameter.ipcSocketDirectory = originalSocketDirectory;
      if (socketFile != null) {
        Files.deleteIfExists(socketFile);
        Files.deleteIfExists(socketFile.getParent());
      }
      Files.deleteIfExists(socketDirectory);
    }
  }

  @Test(timeout = 10_000)
  public void testInnerStartRollsBackWhenAcceptorSubmissionFails() throws Exception {
    assumePosixFileSystem();
    CommonParameter parameter = Args.getInstance();
    String originalOutputDirectory = parameter.outputDirectory;
    String originalSocketDirectory = parameter.ipcSocketDirectory;
    Path outputDirectory = Files.createTempDirectory(Paths.get("/tmp"),
        "ipc-submit-fail-");
    IpcService service = newIpcService();
    Path socketFile = null;
    try {
      parameter.outputDirectory = outputDirectory.toString();
      parameter.ipcSocketDirectory = "";
      socketFile = resolveSocketFilePath(parameter, getPid(service));
      getExecutorService(service, "acceptorExecutor").shutdownNow();

      try {
        service.innerStart();
        Assert.fail("Expected acceptor submission to fail");
      } catch (RejectedExecutionException e) {
        Assert.assertFalse(isRunning(service));
      }

      Assert.assertFalse(Files.exists(socketFile));
      Assert.assertFalse(Files.exists(socketFile.getParent()));
    } finally {
      parameter.outputDirectory = originalOutputDirectory;
      parameter.ipcSocketDirectory = originalSocketDirectory;
      service.innerStop();
      if (socketFile != null) {
        Files.deleteIfExists(socketFile);
        Files.deleteIfExists(socketFile.getParent());
      }
      Files.deleteIfExists(outputDirectory);
    }
  }

  @Test(timeout = 10_000)
  public void testHandlesMultipleClientsConcurrently() throws Exception {
    assumePosixFileSystem();
    CommonParameter parameter = Args.getInstance();
    String originalOutputDirectory = parameter.outputDirectory;
    Path outputDirectory = Files.createTempDirectory(Paths.get("/tmp"), "ipc-multi-client-test-");
    IpcService service = new IpcService(
        new AdminJsonRpcImpl());
    boolean started = false;
    Path socketFile = null;
    try {
      parameter.outputDirectory = outputDirectory.toString();
      service.innerStart();
      started = true;

      socketFile = resolveSocketFilePath(parameter, getPid(service));
      AFUNIXSocketAddress address = AFUNIXSocketAddress.of(socketFile.toFile());
      try (AFUNIXSocket firstClient = AFUNIXSocket.newInstance();
          AFUNIXSocket secondClient = AFUNIXSocket.newInstance()) {
        firstClient.connect(address);
        firstClient.setSoTimeout(5_000);
        BufferedWriter firstWriter = new BufferedWriter(
            new OutputStreamWriter(firstClient.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader firstReader = new BufferedReader(
            new InputStreamReader(firstClient.getInputStream(), StandardCharsets.UTF_8));
        assertSuccessfulResponse(sendRequest(firstWriter, firstReader, 1), 1);
        assertSuccessfulResponse(sendRequest(firstWriter, firstReader, 2), 2);

        secondClient.connect(address);
        secondClient.setSoTimeout(5_000);
        BufferedWriter secondWriter = new BufferedWriter(
            new OutputStreamWriter(secondClient.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader secondReader = new BufferedReader(
            new InputStreamReader(secondClient.getInputStream(), StandardCharsets.UTF_8));
        assertSuccessfulResponse(sendRequest(secondWriter, secondReader, 3), 3);
      }
    } finally {
      cleanupIpcService(service, started, parameter, originalOutputDirectory, socketFile,
          outputDirectory);
    }
  }

  @Test(timeout = 10_000)
  public void testDispatchClientSetsIdleTimeoutAndReleasesFailedHandler() throws Exception {
    IpcService service = newIpcService();
    AFUNIXSocket client = Mockito.mock(AFUNIXSocket.class);
    Mockito.doThrow(new IOException("closed")).when(client).getInputStream();
    try {
      setField(service, "isRunning", true);
      dispatchClient(service, client);

      ExecutorService clientExecutor = getExecutorService(service, "clientExecutor");
      clientExecutor.shutdown();
      Assert.assertTrue("Expected the failed handler to finish before stopping the service",
          clientExecutor.awaitTermination(2, TimeUnit.SECONDS));
      Mockito.verify(client).setSoTimeout(10 * 60 * 1000);
      Mockito.verify(client).close();
      Assert.assertTrue(getActiveClientSockets(service).isEmpty());
    } finally {
      service.innerStop();
    }
  }

  @Test(timeout = 5_000)
  public void testDispatchClientClosesSocketWhenTimeoutConfigurationFails() throws Exception {
    IpcService service = newIpcService();
    AFUNIXSocket client = Mockito.mock(AFUNIXSocket.class);
    Mockito.doThrow(new SocketException("timeout configuration failed"))
        .when(client).setSoTimeout(Mockito.anyInt());
    try {
      setField(service, "isRunning", true);

      dispatchClient(service, client);

      Mockito.verify(client).close();
      Mockito.verify(client, Mockito.never()).getInputStream();
      Assert.assertTrue(getActiveClientSockets(service).isEmpty());
    } finally {
      service.innerStop();
    }
  }

  @Test(timeout = 5_000)
  public void testDispatchClientDoesNotSubmitWhenStopped() throws Exception {
    IpcService service = newIpcService();
    ExecutorService clientExecutor = Mockito.mock(ExecutorService.class);
    AFUNIXSocket client = Mockito.mock(AFUNIXSocket.class);
    service.innerStop();
    setField(service, "clientExecutor", clientExecutor);

    dispatchClient(service, client);

    Mockito.verifyNoInteractions(clientExecutor);
    Mockito.verify(client).close();
    Assert.assertTrue(getActiveClientSockets(service).isEmpty());
  }

  @Test(timeout = 5_000)
  public void testDispatchClientReleasesSocketWhenSubmissionThrows() throws Exception {
    IpcService service = newIpcService();
    ExecutorService clientExecutor = Mockito.mock(ExecutorService.class);
    AFUNIXSocket client = Mockito.mock(AFUNIXSocket.class);
    IllegalStateException failure = new IllegalStateException("submission failed");
    Mockito.when(clientExecutor.submit(Mockito.any(Runnable.class))).thenThrow(failure);
    Mockito.when(clientExecutor.awaitTermination(Mockito.anyLong(), Mockito.any(TimeUnit.class)))
        .thenReturn(true);
    try {
      setField(service, "isRunning", true);
      setField(service, "clientExecutor", clientExecutor);

      try {
        dispatchClient(service, client);
        Assert.fail("Expected the submission failure to be preserved");
      } catch (IllegalStateException e) {
        Assert.assertSame(failure, e);
      }

      Mockito.verify(client).close();
      Assert.assertTrue(getActiveClientSockets(service).isEmpty());
    } finally {
      service.innerStop();
    }
  }

  @Test(timeout = 10_000)
  public void testRejectsClientImmediatelyWhenAllHandlersAreBusy() throws Exception {
    IpcService service = newIpcService();
    CountDownLatch handlersStarted = new CountDownLatch(16);
    CountDownLatch releaseHandlers = new CountDownLatch(1);
    try {
      setField(service, "isRunning", true);
      for (int i = 0; i < 16; i++) {
        AFUNIXSocket client = Mockito.mock(AFUNIXSocket.class);
        Mockito.when(client.getInputStream()).thenAnswer(invocation -> {
          handlersStarted.countDown();
          try {
            releaseHandlers.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          throw new IOException("closed");
        });
        dispatchClient(service, client);
      }
      Assert.assertTrue("Expected all IPC handlers to start without queueing",
          handlersStarted.await(5, TimeUnit.SECONDS));

      AFUNIXSocket rejectedClient = Mockito.mock(AFUNIXSocket.class);
      dispatchClient(service, rejectedClient);

      Mockito.verify(rejectedClient).close();
      Assert.assertFalse(getActiveClientSockets(service).contains(rejectedClient));
    } finally {
      releaseHandlers.countDown();
      service.innerStop();
    }
  }

  @Test(timeout = 10_000)
  public void testStopClosesActiveClientSocket() throws Exception {
    assumePosixFileSystem();
    CommonParameter parameter = Args.getInstance();
    String originalOutputDirectory = parameter.outputDirectory;
    Path outputDirectory = Files.createTempDirectory(Paths.get("/tmp"), "ipc-test-");
    IpcService service = new IpcService(
        new AdminJsonRpcImpl());
    boolean started = false;
    Path socketFile = null;
    try {
      parameter.outputDirectory = outputDirectory.toString();
      service.innerStart();
      started = true;

      socketFile = resolveSocketFilePath(parameter, getPid(service));
      AFUNIXSocketAddress address = AFUNIXSocketAddress.of(socketFile.toFile());
      try (AFUNIXSocket client = AFUNIXSocket.newInstance()) {
        client.connect(address);
        client.setSoTimeout(5_000);
        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
          writer.write("{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
              + "\"params\":[\"a\",\"b\"],\"id\":1}");
          writer.newLine();
          writer.flush();
          Assert.assertNotNull(reader.readLine());

          service.innerStop();
          started = false;
        }
      }
    } finally {
      cleanupIpcService(service, started, parameter, originalOutputDirectory, socketFile,
          outputDirectory);
    }
  }

  @Test(timeout = 5_000)
  public void testStopDoesNotWaitForUnresponsiveClientWorker() throws Exception {
    IpcService service = newIpcService();
    ExecutorService clientExecutor = getExecutorService(service, "clientExecutor");
    CountDownLatch workerStarted = new CountDownLatch(1);
    CountDownLatch releaseWorker = new CountDownLatch(1);
    clientExecutor.submit(() -> {
      workerStarted.countDown();
      boolean interrupted = false;
      while (true) {
        try {
          releaseWorker.await();
          break;
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    });
    Assert.assertTrue("Expected the client worker to start",
        workerStarted.await(2, TimeUnit.SECONDS));

    try {
      service.innerStop();

      Assert.assertTrue(clientExecutor.isShutdown());
      Assert.assertFalse("The unresponsive worker should still be running",
          clientExecutor.isTerminated());
    } finally {
      releaseWorker.countDown();
      Assert.assertTrue("Expected the released client worker to terminate",
          clientExecutor.awaitTermination(2, TimeUnit.SECONDS));
    }
  }

  @Test(timeout = 5_000)
  public void testInnerStopContinuesCleanupAfterServerCloseFailure() throws Exception {
    IpcService service = newIpcService();
    AFUNIXServerSocket serverSocket = Mockito.mock(AFUNIXServerSocket.class);
    AFUNIXSocket clientSocket = Mockito.mock(AFUNIXSocket.class);
    Path socketRootDirectory = Files.createTempDirectory("ipc-stop-failure-test-");
    Path socketDirectory = Files.createDirectory(socketRootDirectory.resolve("ipc"));
    Path socketFile = Files.createFile(socketDirectory.resolve("1234.sock"));
    Mockito.doThrow(new IOException("server close failed")).when(serverSocket).close();
    setField(service, "unixServerSocket", serverSocket);
    setField(service, "socketFilePath", socketFile);
    setField(service, "socketDirectoryPath", socketDirectory);
    getActiveClientSockets(service).add(clientSocket);

    try {
      try {
        service.innerStop();
        Assert.fail("Expected the server socket close failure to be preserved");
      } catch (IOException e) {
        Assert.assertEquals("server close failed", e.getMessage());
      }

      Mockito.verify(clientSocket).shutdownInput();
      Mockito.verify(clientSocket).shutdownOutput();
      Mockito.verify(clientSocket).close();
      Assert.assertTrue(getActiveClientSockets(service).isEmpty());
      Assert.assertTrue(getExecutorService(service, "acceptorExecutor").isShutdown());
      Assert.assertTrue(getExecutorService(service, "clientExecutor").isShutdown());
      Assert.assertFalse(Files.exists(socketFile));
      Assert.assertFalse(Files.exists(socketDirectory));
    } finally {
      Files.deleteIfExists(socketFile);
      Files.deleteIfExists(socketDirectory);
      Files.deleteIfExists(socketRootDirectory);
    }
  }

  @Test(timeout = 5_000)
  public void testInnerStopContinuesAfterClientCloseFailures() throws Exception {
    IpcService service = newIpcService();
    AFUNIXSocket[] clients = {
        Mockito.mock(AFUNIXSocket.class), Mockito.mock(AFUNIXSocket.class)
    };
    for (AFUNIXSocket client : clients) {
      Mockito.doThrow(new IOException("input shutdown failed")).when(client).shutdownInput();
      Mockito.doThrow(new IOException("output shutdown failed")).when(client).shutdownOutput();
      Mockito.doThrow(new IOException("client close failed")).when(client).close();
      getActiveClientSockets(service).add(client);
    }

    try {
      service.innerStop();

      for (AFUNIXSocket client : clients) {
        Mockito.verify(client).shutdownInput();
        Mockito.verify(client).shutdownOutput();
        Mockito.verify(client).close();
      }
      Assert.assertTrue(getActiveClientSockets(service).isEmpty());
      Assert.assertTrue(getExecutorService(service, "acceptorExecutor").isShutdown());
      Assert.assertTrue(getExecutorService(service, "clientExecutor").isShutdown());
    } finally {
      service.innerStop();
    }
  }

  @Test(timeout = 5_000)
  public void testInnerStopSuppressesLaterCleanupFailure() throws Exception {
    IpcService service = newIpcService();
    AFUNIXServerSocket serverSocket = Mockito.mock(AFUNIXServerSocket.class);
    Path socketRootDirectory = Files.createTempDirectory("ipc-stop-suppressed-test-");
    Path socketDirectory = Files.createDirectory(
        socketRootDirectory.resolve("ipc"));
    Path childFile = Files.createFile(socketDirectory.resolve("child"));
    Mockito.doThrow(new IOException("server close failed")).when(serverSocket).close();
    setField(service, "unixServerSocket", serverSocket);
    setField(service, "socketFilePath", socketDirectory.resolve("1234.sock"));
    setField(service, "socketDirectoryPath", socketDirectory);

    try {
      service.innerStop();
      Assert.fail("Expected cleanup failures to be preserved");
    } catch (IOException e) {
      Assert.assertEquals("server close failed", e.getMessage());
      Assert.assertEquals(1, e.getSuppressed().length);
      Assert.assertTrue(e.getSuppressed()[0] instanceof IOException);
      Assert.assertTrue("Unrelated files must survive cleanup", Files.exists(childFile));
    } finally {
      Files.deleteIfExists(childFile);
      Files.deleteIfExists(socketDirectory);
      Files.deleteIfExists(socketRootDirectory);
    }
  }

  @Test
  public void testCleanupRestoresOutputDirectoryWhenStopFails() throws Exception {
    CommonParameter parameter = new CommonParameter();
    String originalOutputDirectory = parameter.outputDirectory;
    Path outputDirectory = Files.createTempDirectory("ipc-cleanup-test-");
    Path socketDirectory = Files.createDirectory(outputDirectory.resolve("ipc"));
    Path socketFile = Files.createFile(socketDirectory.resolve("1234.sock"));
    parameter.outputDirectory = outputDirectory.toString();
    IpcService service = Mockito.mock(IpcService.class);
    Mockito.doThrow(new IOException("stop failed")).when(service).innerStop();

    try {
      cleanupIpcService(service, true, parameter, originalOutputDirectory, socketFile,
          outputDirectory);
      Assert.fail("Expected the stop failure to be preserved");
    } catch (IOException e) {
      Assert.assertEquals("stop failed", e.getMessage());
    }

    Assert.assertEquals(originalOutputDirectory, parameter.outputDirectory);
    Assert.assertFalse(Files.exists(socketFile));
    Assert.assertFalse(Files.exists(socketDirectory));
    Assert.assertFalse(Files.exists(outputDirectory));
  }

  private IpcService newIpcService() {
    return new IpcService(new AdminJsonRpcImpl());
  }

  private void assertStartupRejectsExistingDirectory(IpcService service) throws Exception {
    try {
      service.innerStart();
      Assert.fail("Expected startup to reject an existing IPC directory");
    } catch (TronError e) {
      Assert.assertEquals(TronError.ErrCode.API_SERVER_INIT, e.getErrCode());
      Assert.assertTrue(e.getMessage().contains("IPC directory already exists"));
      Assert.assertTrue(e.getMessage().contains("remove it manually"));
      Assert.assertFalse(isRunning(service));
    }
  }

  private void assertNewConnectionSucceeds(Path socketFile) throws IOException {
    try (AFUNIXSocket client = AFUNIXSocket.newInstance()) {
      client.connect(AFUNIXSocketAddress.of(socketFile.toFile()));
      client.setSoTimeout(2_000);
      BufferedWriter writer = new BufferedWriter(
          new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
      assertSuccessfulResponse(sendRequest(writer, reader, 1), 1);
    }
  }

  private void cleanupIpcService(IpcService service, boolean started, CommonParameter parameter,
      String originalOutputDirectory, Path socketFile, Path outputDirectory) throws Exception {
    parameter.outputDirectory = originalOutputDirectory;
    Exception failure = null;
    if (started) {
      try {
        service.innerStop();
      } catch (Exception e) {
        failure = e;
      }
    }
    try {
      if (socketFile != null) {
        Files.deleteIfExists(socketFile);
        Files.deleteIfExists(socketFile.getParent());
      }
    } catch (IOException e) {
      failure = mergeCleanupFailure(failure, e);
    }
    try {
      Files.deleteIfExists(outputDirectory);
    } catch (IOException e) {
      failure = mergeCleanupFailure(failure, e);
    }
    if (failure != null) {
      throw failure;
    }
  }

  private Exception mergeCleanupFailure(Exception failure, IOException cleanupFailure) {
    if (failure == null) {
      return cleanupFailure;
    }
    failure.addSuppressed(cleanupFailure);
    return failure;
  }

  private Path resolveSocketFilePath(CommonParameter parameter, String pid) {
    return new IpcSocketFiles().resolveSocketFilePath(parameter.getOutputDirectory(),
        parameter.getIpcSocketDirectory(), pid);
  }

  private boolean isRunning(IpcService service) throws Exception {
    Field field = IpcService.class.getDeclaredField("isRunning");
    field.setAccessible(true);
    return field.getBoolean(service);
  }

  private ExecutorService getExecutorService(IpcService service, String fieldName)
      throws Exception {
    Field field = IpcService.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return (ExecutorService) field.get(service);
  }

  @SuppressWarnings("unchecked")
  private Set<AFUNIXSocket> getActiveClientSockets(IpcService service) throws Exception {
    Field field = IpcService.class.getDeclaredField("activeClientSockets");
    field.setAccessible(true);
    return (Set<AFUNIXSocket>) field.get(service);
  }

  private void setField(IpcService service, String fieldName, Object value) throws Exception {
    Field field = IpcService.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(service, value);
  }

  private String getPid(IpcService service) throws Exception {
    return (String) invokePrivate(service, "getPid", new Class<?>[0]);
  }

  private void dispatchClient(IpcService service, AFUNIXSocket client) throws Exception {
    invokePrivate(service, "dispatchClient", new Class<?>[] {AFUNIXSocket.class}, client);
  }

  private Object invokePrivate(IpcService service, String methodName, Class<?>[] parameterTypes,
      Object... arguments) throws Exception {
    Method method = IpcService.class.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    try {
      return method.invoke(service, arguments);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw new IllegalStateException(cause);
    }
  }

  private String sendRequest(BufferedWriter writer, BufferedReader reader, int requestId)
      throws IOException {
    writer.write("{\"jsonrpc\":\"2.0\",\"method\":\"admin_example\","
        + "\"params\":[\"a\",\"b\"],\"id\":" + requestId + "}");
    writer.newLine();
    writer.flush();
    return reader.readLine();
  }

  private void assertSuccessfulResponse(String response, int requestId) {
    Assert.assertNotNull(response);
    Assert.assertTrue(response, response.contains("\"result\":\"a:b\""));
    Assert.assertTrue(response, response.contains("\"id\":" + requestId));
  }

  private void assumePosixFileSystem() {
    Assume.assumeTrue("IPC requires POSIX file permissions",
        FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
  }
}
