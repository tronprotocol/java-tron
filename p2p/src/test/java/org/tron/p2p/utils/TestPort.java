package org.tron.p2p.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

/**
 * Picks a free port for tests that need to bind one.
 *
 * <p>A fixed port collides between test classes and between the parallel forks
 * Gradle runs. PeerServer.start only logs on bind failure, so a collision lets a
 * test pass while exercising nothing. framework's own tests use
 * org.tron.common.utils.PublicMethod.chooseRandomPort for this; :p2p cannot
 * depend on :framework — that would be a cycle — so the same few lines live here.
 */
public class TestPort {

  private static final int MIN = 10240;
  private static final int MAX = 65000;
  private static final Random RANDOM = new Random();

  private TestPort() {
  }

  public static int choose() {
    int port = next();
    try {
      while (!available(port)) {
        port = next();
      }
    } catch (IOException e) {
      return next();
    }
    return port;
  }

  private static int next() {
    return RANDOM.nextInt(MAX - MIN + 1) + MIN;
  }

  private static boolean available(int port) throws IOException {
    try (ServerSocket socket = new ServerSocket(port)) {
      socket.setReuseAddress(true);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
