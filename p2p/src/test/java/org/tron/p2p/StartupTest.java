package org.tron.p2p;

import java.io.File;
import java.lang.reflect.Field;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.example.StartApp;

public class StartupTest {

  @Test(timeout = 45000)
  public void bindFailureReachesCallerAndDoesNotLeaveProcessRunning() throws Exception {
    try (ServerSocket occupied = new ServerSocket(0)) {
      String output = run(StartupProbe.class, 0, "failure",
          Integer.toString(occupied.getLocalPort()));
      Assert.assertTrue(output.contains("BIND_FAILURE_PROPAGATED"));
    }
  }

  @Test(timeout = 45000)
  public void cleanupFailureDoesNotReplaceBindFailure() throws Exception {
    try (ServerSocket occupied = new ServerSocket(0)) {
      String output = run(StartupProbe.class, 0, "cleanup-failure",
          Integer.toString(occupied.getLocalPort()));
      Assert.assertTrue(output.contains("BIND_FAILURE_PROPAGATED"));
    }
  }

  @Test(timeout = 45000)
  public void startAppExitsWithFailureStatusWhenPortIsOccupied() throws Exception {
    try (ServerSocket occupied = new ServerSocket(0)) {
      String output = run(StartApp.class, 1, "-p", Integer.toString(occupied.getLocalPort()));
      Assert.assertTrue(output.contains("P2P service startup failed"));
      Assert.assertTrue(output.contains(Integer.toString(occupied.getLocalPort())));
    }
  }

  @Test(timeout = 45000)
  public void successfulServiceCanStartAndClose() throws Exception {
    Assert.assertTrue(run(StartupProbe.class, 0, "success").contains("STARTED_AND_CLOSED"));
  }

  private String run(Class<?> mainClass, int expectedExit, String... args) throws Exception {
    List<String> command = new ArrayList<>();
    command.add(new File(System.getProperty("java.home"), "bin/java").getPath());
    command.add("-Dio.netty.eventLoopThreads=2");
    command.add("-cp");
    command.add(classPath());
    command.add(mainClass.getName());
    command.addAll(Arrays.asList(args));
    Path outputFile = Files.createTempFile("libp2p-startup-test-", ".log");
    Process process = null;
    try {
      process = new ProcessBuilder(command).redirectErrorStream(true)
          .redirectOutput(outputFile.toFile()).start();
      boolean exited = process.waitFor(35, TimeUnit.SECONDS);
      String output = new String(Files.readAllBytes(outputFile), StandardCharsets.UTF_8);
      Assert.assertTrue("Process did not exit; startup resources may remain active:\n" + output,
          exited);
      Assert.assertEquals(output, expectedExit, process.exitValue());
      return output;
    } finally {
      if (process != null && process.isAlive()) {
        process.destroyForcibly();
        process.waitFor(5, TimeUnit.SECONDS);
      }
      Files.deleteIfExists(outputFile);
    }
  }

  private String classPath() throws Exception {
    Set<String> paths = new LinkedHashSet<>();
    paths.addAll(Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)));
    for (ClassLoader loader = getClass().getClassLoader(); loader != null;
        loader = loader.getParent()) {
      if (loader instanceof URLClassLoader) {
        for (URL url : ((URLClassLoader) loader).getURLs()) {
          if ("file".equals(url.getProtocol())) {
            paths.add(new File(url.toURI()).getPath());
          }
        }
      }
    }
    return String.join(File.pathSeparator, paths);
  }

  // Runs in a separate JVM so natural process exit verifies cleanup of non-daemon threads.
  public static class StartupProbe {

    public static void main(String[] args) throws Exception {
      P2pConfig config = new P2pConfig();
      config.setIp("127.0.0.1");
      config.setIpv6("");
      config.setMinConnections(0);
      config.setMinActiveConnections(0);
      config.setDiscoverEnable(true);
      boolean cleanupFailure = "cleanup-failure".equals(args[0]);
      RuntimeException cleanupError = new IllegalStateException("Cleanup test failure");
      P2pService service = cleanupFailure ? new P2pService() {
        @Override
        public void close() {
          super.close();
          throw cleanupError;
        }
      } : new P2pService();
      if ("success".equals(args[0])) {
        try (ServerSocket available = new ServerSocket(0)) {
          config.setPort(available.getLocalPort());
        }
        service.start(config);
        Assert.assertFalse(ChannelManager.isShutdown);
        service.close();
        service.close();
        System.out.println("STARTED_AND_CLOSED");
        return;
      }

      config.setPort(Integer.parseInt(args[1]));
      IllegalStateException error = Assert.assertThrows(IllegalStateException.class,
          () -> service.start(config));
      Assert.assertTrue(error.getCause() instanceof BindException);
      Assert.assertTrue(ChannelManager.isShutdown);
      Field workerGroup = ChannelManager.getPeerClient().getClass().getDeclaredField("workerGroup");
      workerGroup.setAccessible(true);
      Assert.assertNull(workerGroup.get(ChannelManager.getPeerClient()));
      if (cleanupFailure) {
        Assert.assertEquals(1, error.getSuppressed().length);
        Assert.assertSame(cleanupError, error.getSuppressed()[0]);
      } else {
        Assert.assertEquals(0, error.getSuppressed().length);
      }
      System.out.println("BIND_FAILURE_PROPAGATED");
    }
  }
}
