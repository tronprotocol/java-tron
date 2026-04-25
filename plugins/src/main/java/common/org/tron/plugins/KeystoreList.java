package org.tron.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.tron.keystore.WalletFile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "list",
    mixinStandardHelpOptions = true,
    description = "List all keystore files in a directory.")
public class KeystoreList implements Callable<Integer> {

  private static final ObjectMapper MAPPER = KeystoreCliUtils.mapper();

  @Spec
  private CommandSpec spec;

  @Option(names = {"--keystore-dir"},
      description = "Keystore directory (default: ./Wallet)",
      defaultValue = "Wallet")
  private File keystoreDir;

  @Option(names = {"--json"},
      description = "Output in JSON format")
  private boolean json;

  @Override
  public Integer call() {
    PrintWriter out = spec.commandLine().getOut();
    PrintWriter err = spec.commandLine().getErr();

    if (!keystoreDir.exists() || !keystoreDir.isDirectory()) {
      if (json) {
        return printEmptyJson(out, err);
      } else {
        out.println("No keystores found in: " + keystoreDir.getAbsolutePath());
      }
      return 0;
    }

    File[] files = keystoreDir.listFiles((dir, name) -> name.endsWith(".json"));
    if (files == null || files.length == 0) {
      if (json) {
        return printEmptyJson(out, err);
      } else {
        out.println("No keystores found in: " + keystoreDir.getAbsolutePath());
      }
      return 0;
    }

    List<Map<String, String>> entries = new ArrayList<>();
    for (File file : files) {
      byte[] bytes = KeystoreCliUtils.readKeystoreFile(file, err);
      if (bytes == null) {
        continue;
      }
      try {
        WalletFile walletFile = MAPPER.readValue(bytes, WalletFile.class);
        if (!KeystoreCliUtils.isValidKeystoreFile(walletFile)) {
          continue;
        }
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("address", walletFile.getAddress());
        entry.put("file", file.getName());
        entries.add(entry);
      } catch (Exception e) {
        err.println("Warning: skipping unreadable file: " + file.getName());
      }
    }

    if (json) {
      try {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keystores", entries);
        out.println(MAPPER.writeValueAsString(result));
      } catch (Exception e) {
        err.println("Error writing JSON output");
        return 1;
      }
    } else if (entries.isEmpty()) {
      out.println("No valid keystores found in: " + keystoreDir.getAbsolutePath());
    } else {
      for (Map<String, String> entry : entries) {
        out.printf("%-45s %s%n", entry.get("address"), entry.get("file"));
      }
    }
    return 0;
  }

  private int printEmptyJson(PrintWriter out, PrintWriter err) {
    try {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("keystores", new ArrayList<>());
      out.println(MAPPER.writeValueAsString(result));
      return 0;
    } catch (Exception e) {
      err.println("Error writing JSON output");
      return 1;
    }
  }
}
