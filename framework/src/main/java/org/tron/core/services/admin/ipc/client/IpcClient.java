package org.tron.core.services.admin.ipc.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.SyntaxError;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.tron.core.services.admin.AdminJsonRpc;
import org.tron.core.services.admin.ipc.client.IpcConsoleCommands.Action;
import org.tron.core.services.admin.ipc.client.IpcConsoleCommands.Command;
import org.tron.program.Version;

/**
 * Owns the IPC connection and console session for interactive and single-command execution.
 * Command preparation and response formatting are shared by both modes.
 *
 * <p>Keep this class independent of SLF4J, including Lombok's {@code @Slf4j}. Client diagnostics
 * must be written to the console through {@link System#out}, {@link System#err}, or JLine so the
 * client does not initialize or write to the node's Logback appenders.
 */
public class IpcClient {

  private static final int EXEC_RESPONSE_TIMEOUT_MILLIS = 30_000;
  static final int EXIT_SUCCESS = 0;
  static final int EXIT_FAILURE = 1;

  private final String socketFilePath;
  private final IpcConsoleCommands commands = new IpcConsoleCommands(AdminJsonRpc.class);

  public IpcClient(String socketFilePath) {
    this.socketFilePath = socketFilePath;
  }

  public int start(String execCommand) {
    try {
      return run(execCommand);
    } catch (IOException e) {
      System.err.println("Failed to communicate with IPC server.");
      return EXIT_FAILURE;
    }
  }

  public int run() throws IOException {
    return run(null);
  }

  int run(String execCommand) throws IOException {
    File socketFile = new File(socketFilePath);
    if (!socketFile.exists()) {
      System.err.println("Error: IPC socket file does not exist: " + socketFile.getName());
      return EXIT_FAILURE;
    }
    AFUNIXSocketAddress address = AFUNIXSocketAddress.of(socketFile);
    try (Socket socket = AFUNIXSocket.newInstance()) {
      socket.connect(address);
      if (execCommand != null) {
        return runExec(socket, execCommand);
      }
      printWelcome(socketFile);
      try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
        LineReader reader = createLineReader(terminal);
        runSession(socket, reader);
      }
      return EXIT_SUCCESS;
    }
  }

  int runExec(Socket socket, String commandLine) throws IOException {
    Command command = prepareCommand(commandLine);
    switch (command.getAction()) {
      case EMPTY:
        System.err.println("No command specified for --exec.");
        return EXIT_FAILURE;
      case EXIT:
      case HELP:
        return EXIT_SUCCESS;
      case ERROR:
        return EXIT_FAILURE;
      case REQUEST:
        break;
      default:
        throw new IllegalStateException("Unexpected IPC command action");
    }

    socket.setSoTimeout(EXEC_RESPONSE_TIMEOUT_MILLIS);
    try (BufferedWriter serverWriter = new BufferedWriter(
        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader serverReader = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
      sendRequest(serverWriter, command.getRequest());

      String response;
      do {
        try {
          response = serverReader.readLine();
        } catch (SocketTimeoutException e) {
          System.err.println("Timed out waiting for IPC response.");
          return EXIT_FAILURE;
        }
      } while (response != null && response.trim().isEmpty());
      if (response == null) {
        System.err.println("Disconnected from server before receiving a response.");
        return EXIT_FAILURE;
      }
      IpcResponse parsedResponse = IpcResponse.parse(response, command.getRequestId());
      if (parsedResponse.isSuccessful()) {
        System.out.println(parsedResponse.getFormatted());
        return EXIT_SUCCESS;
      }
      System.err.println(parsedResponse.getFormatted());
      return EXIT_FAILURE;
    }
  }

  void runSession(Socket socket, LineReader reader) throws IOException {
    AtomicBoolean connected = new AtomicBoolean(true);
    startResponseReader(socket, reader, connected, Thread.currentThread());
    try {
      readCommands(socket, reader, connected);
    } finally {
      // Mark a local exit before run() closes the socket, so it cannot look like a remote loss.
      connected.set(false);
    }
  }

  /** Receives responses without blocking terminal input; remote loss wakes the input thread. */
  private void startResponseReader(final Socket socket, LineReader reader, AtomicBoolean connected,
      Thread inputThread) throws IOException {
    final BufferedReader serverReader = new BufferedReader(
        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

    Thread readerThread = new Thread(() -> {
      try {
        String response;
        while ((response = serverReader.readLine()) != null) {
          if (!response.trim().isEmpty()) {
            reader.printAbove(IpcResponse.parse(response).getFormatted());
          }
        }
      } catch (IOException e) {
        // The socket closing is reported to the console by notifyDisconnected below.
      } finally {
        if (notifyDisconnected(connected, reader)) {
          inputThread.interrupt();
        }
      }
    }, "admin-ipc-client-reader");
    readerThread.setDaemon(true);
    readerThread.start();
  }

  private LineReader createLineReader(Terminal terminal) {
    Completer commandCompleter =
        new IpcCommandCompleter(commands.getCompletionCommandNames());
    ArgumentCompleter completer = new ArgumentCompleter(
        commandCompleter,
        NullCompleter.INSTANCE
    );
    return LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(completer)
        .parser(commands.getParser())
        .variable(LineReader.INDENTATION, 2)
        .option(LineReader.Option.AUTO_FRESH_LINE, true)
        .option(LineReader.Option.CASE_INSENSITIVE, true)
        .option(LineReader.Option.HISTORY_IGNORE_DUPS, true)
        .option(LineReader.Option.HISTORY_REDUCE_BLANKS, true)
        .build();
  }

  void printWelcome(File socketFile) {
    System.out.println("Welcome to the java-tron admin console.");
    System.out.println("Client: java-tron/" + Version.getVersion());
    System.out.println("IPC endpoint: " + socketFile.getAbsolutePath());
    System.out.println("Type \"help\" for available commands; \"exit\" or Ctrl-D to quit.");
  }

  /** Reads commands until local exit or disconnection. The enclosing run() owns the socket. */
  private void readCommands(Socket socket, LineReader reader, AtomicBoolean connected) {
    try {
      BufferedWriter serverWriter = new BufferedWriter(
          new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
      while (connected.get()) {
        try {
          Command command = prepareCommand(reader.readLine("> "));
          if (command.getAction() == Action.EXIT) {
            break;
          }
          if (command.getAction() == Action.REQUEST) {
            sendRequest(serverWriter, command.getRequest());
          }
        } catch (SyntaxError e) {
          // JLine can reject input before returning a line to the command parser.
          System.err.println("Invalid command syntax.");
        } catch (IllegalArgumentException e) {
          System.err.println(e.getMessage());
        }
      }
    } catch (UserInterruptException | EndOfFileException e) {
      // Ctrl-C, Ctrl-D, or server disconnection ends the interactive session.
    } catch (IOException e) {
      notifyDisconnected(connected, reader);
    }
  }

  private Command prepareCommand(String commandLine) {
    Command command = commands.prepare(commandLine);
    if (command.getError() != null) {
      System.err.println(command.getError());
    }
    if (command.getOutput() != null) {
      System.out.println(command.getOutput());
    }
    return command;
  }

  private void sendRequest(BufferedWriter writer, String request) throws IOException {
    writer.write(request);
    writer.newLine();
    writer.flush();
  }

  private boolean notifyDisconnected(AtomicBoolean connected, LineReader reader) {
    if (connected.compareAndSet(true, false)) {
      reader.printAbove("Disconnected from server.");
      return true;
    }
    return false;
  }
}
