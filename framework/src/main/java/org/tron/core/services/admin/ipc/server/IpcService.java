package org.tron.core.services.admin.ipc.server;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.application.AbstractService;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.exit.ExitManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.admin.ipc.server.IpcRequestHandler.RequestTooLargeException;

/**
 * Provides the local Admin JSON-RPC endpoint over a Unix domain socket. It accepts and dispatches
 * client connections and owns the startup, worker, and cleanup lifecycle. Filesystem operations
 * and request handling are delegated to package-local components.
 */
@Component
@Slf4j(topic = "API")
public class IpcService extends AbstractService {

  private static final String ACCEPTOR_EXECUTOR_NAME = "admin-ipc-acceptor";
  private static final String CLIENT_EXECUTOR_NAME = "admin-ipc-client";
  private static final int CLIENT_IDLE_TIMEOUT_MILLIS = 10 * 60 * 1000;
  private static final int EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 1;

  private final IpcSocketFiles socketFiles = new IpcSocketFiles();
  private final IpcRequestHandler requestHandler;

  private final ExecutorService acceptorExecutor =
      ExecutorServiceManager.newSingleThreadExecutor(ACCEPTOR_EXECUTOR_NAME, true);
  private final ExecutorService clientExecutor =
      ExecutorServiceManager.newThreadPoolExecutor(4, 16, 60L, TimeUnit.SECONDS,
          new SynchronousQueue<>(), CLIENT_EXECUTOR_NAME, true);

  private final Set<AFUNIXSocket> activeClientSockets = ConcurrentHashMap.newKeySet();
  private AFUNIXServerSocket unixServerSocket;
  private Path socketFilePath;

  private volatile boolean isRunning;

  @Autowired
  public IpcService(AdminJsonRpc adminJsonRpc) {
    enable = isFullNode() && Args.getInstance().isIpcEnable();
    requestHandler = new IpcRequestHandler(adminJsonRpc, Args.getInstance().maxMessageSize);
  }

  @Override
  public CompletableFuture<Boolean> start() {
    CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
    try {
      innerStart();
      resultFuture.complete(true);
    } catch (Exception e) {
      resultFuture.completeExceptionally(e);
    }
    return resultFuture;
  }

  @Override
  public void innerStart() throws Exception {
    CommonParameter parameter = Args.getInstance();
    socketFilePath = socketFiles.resolveSocketFilePath(parameter.getOutputDirectory(),
        parameter.getIpcSocketDirectory(), getPid());
    Path socketDirectory = socketFilePath.getParent();
    socketFiles.validateSocketRootDirectory(socketDirectory.getParent());
    try {
      socketFiles.recreateSocketDirectory(socketDirectory);
      File socketFile = socketFilePath.toFile();
      AFUNIXSocketAddress address = AFUNIXSocketAddress.of(socketFile);
      unixServerSocket = AFUNIXServerSocket.bindOn(address);
      socketFiles.setOwnerOnlyPermissions(socketFilePath);
      unixServerSocket.setShutdownOnClose(true);

      logger.info("IpcService started, listening on {}", socketFile.getAbsolutePath());
    } catch (IOException | RuntimeException e) {
      throw rollbackStartup(e);
    }
    Runnable runnable = () -> {
      while (isRunning) {
        try {
          dispatchClient(unixServerSocket.accept());
        } catch (Throwable throwable) {
          ExitManager.findTronError(throwable).ifPresent(e -> {
            throw e;
          });
          if (isRunning) {
            logger.error("Handle IPC request error", throwable);
            try {
              TimeUnit.MILLISECONDS.sleep(5_000);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              break;
            }
          }
        }
      }
    };
    isRunning = true;
    try {
      ExecutorServiceManager.submit(acceptorExecutor, runnable);
    } catch (RuntimeException e) {
      isRunning = false;
      throw rollbackStartup(e);
    }
  }

  private void dispatchClient(AFUNIXSocket client) {
    boolean submitted = false;
    try {
      client.setSoTimeout(CLIENT_IDLE_TIMEOUT_MILLIS);
      activeClientSockets.add(client);
      if (!isRunning) {
        return;
      }
      ExecutorServiceManager.submit(clientExecutor, () -> handleClient(client));
      submitted = true;
    } catch (IOException e) {
      if (isRunning) {
        logger.warn("Failed to configure IPC client idle timeout");
      }
    } catch (RejectedExecutionException e) {
      if (isRunning) {
        logger.warn("Too many IPC clients; rejecting connection");
      }
    } finally {
      // Once submitted, handleClient owns the connection and releases it on exit.
      if (!submitted) {
        closeClient(client);
      }
    }
  }

  private void handleClient(AFUNIXSocket client) {
    try (BufferedInputStream input = new BufferedInputStream(client.getInputStream());
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

      String line;
      while ((line = requestHandler.readRequest(input)) != null) {
        String cmd = line.trim();
        logger.debug("Received IPC request");
        String response = requestHandler.handleCommand(cmd);
        if (!response.isEmpty()) {
          writer.write(response);
          writer.newLine();
          writer.flush();
          logger.debug("Sent IPC response");
        }
      }
    } catch (SocketTimeoutException e) {
      logger.debug("Closing IPC client after {} ms without input", CLIENT_IDLE_TIMEOUT_MILLIS);
    } catch (RequestTooLargeException e) {
      logger.warn("IPC request exceeds maximum size {} bytes", requestHandler.getMaxRequestSize());
    } catch (IOException e) {
      if (isRunning) {
        logger.error("Client disconnected {}", client);
      }
    } finally {
      closeClient(client);
    }
  }

  @Override
  public void innerStop() throws Exception {
    logger.info("Begin to stop IpcService ...");
    isRunning = false;

    Exception failure = runCleanup(null,
        this::closeServerSocket,
        this::shutdownActiveClients,
        this::shutdownExecutors,
        activeClientSockets::clear,
        () -> socketFiles.deleteSocketFile(socketFilePath),
        () -> socketFiles.deleteSocketDirectory(socketFilePath));

    if (failure != null) {
      throw failure;
    }
    logger.info("IpcService stopped");
  }

  private void closeServerSocket() throws IOException {
    if (unixServerSocket != null) {
      unixServerSocket.close();
    }
  }

  private void shutdownActiveClients() {
    for (AFUNIXSocket client : activeClientSockets) {
      // Wake blocked reads and writes before closing the socket from the stopping thread.
      try {
        client.shutdownInput();
      } catch (IOException e) {
        logger.debug("Failed to shut down IPC client input");
      }
      try {
        client.shutdownOutput();
      } catch (IOException e) {
        logger.debug("Failed to shut down IPC client output");
      }
      closeClient(client);
    }
  }

  private void shutdownExecutors() {
    // Closing a junixsocket from another thread does not always wake a native read promptly. The
    // workers are daemon threads, so interrupt them and use a short bounded wait instead of the
    // shared executor shutdown helper's 60-second wait.
    acceptorExecutor.shutdownNow();
    clientExecutor.shutdownNow();
    awaitExecutorTermination(acceptorExecutor, ACCEPTOR_EXECUTOR_NAME);
    awaitExecutorTermination(clientExecutor, CLIENT_EXECUTOR_NAME);
  }

  private void awaitExecutorTermination(ExecutorService executor, String name) {
    try {
      if (!executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        logger.warn("Pool {} did not terminate within {} second", name,
            EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private Exception rollbackStartup(Exception failure) {
    if (unixServerSocket != null) {
      failure = runCleanup(failure, this::closeServerSocket,
          () -> socketFiles.deleteSocketFile(socketFilePath));
    }
    return runCleanup(failure, () -> socketFiles.deleteSocketDirectory(socketFilePath));
  }

  /**
   * Runs every cleanup action in order, retaining the first failure and suppressing later ones.
   */
  private Exception runCleanup(Exception failure, CleanupAction... actions) {
    for (CleanupAction action : actions) {
      try {
        action.run();
      } catch (Exception cleanupFailure) {
        if (failure == null) {
          failure = cleanupFailure;
        } else {
          failure.addSuppressed(cleanupFailure);
        }
      }
    }
    return failure;
  }

  private void closeClient(AFUNIXSocket client) {
    try {
      client.close();
    } catch (IOException e) {
      logger.warn("Failed to close IPC client socket");
    } finally {
      activeClientSockets.remove(client);
    }
  }

  private String getPid() {
    String name = ManagementFactory.getRuntimeMXBean().getName();
    return name.split("@")[0];
  }

  @FunctionalInterface
  private interface CleanupAction {

    void run() throws Exception;
  }
}
