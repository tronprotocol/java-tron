package org.tron.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.tron.keystore.WalletFile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list",
    mixinStandardHelpOptions = true,
    description = "List all keystore files in a directory.")
public class KeystoreList implements Callable<Integer> {

  private static final ObjectMapper MAPPER = KeystoreCliUtils.mapper();

  @Option(names = {"--keystore-dir"},
      description = "Keystore directory (default: ./Wallet)",
      defaultValue = "Wallet")
  private File keystoreDir;

  @Option(names = {"--json"},
      description = "Output in JSON format")
  private boolean json;

  @Override
  public Integer call() {
    if (!keystoreDir.exists() || !keystoreDir.isDirectory()) {
      if (json) {
        return printEmptyJson();
      } else {
        System.out.println("No keystores found in: " + keystoreDir.getAbsolutePath());
      }
      return 0;
    }

    File[] files = keystoreDir.listFiles((dir, name) -> name.endsWith(".json"));
    if (files == null || files.length == 0) {
      if (json) {
        return printEmptyJson();
      } else {
        System.out.println("No keystores found in: " + keystoreDir.getAbsolutePath());
      }
      return 0;
    }

    List<Map<String, String>> entries = new ArrayList<>();
    for (File file : files) {
      try {
        WalletFile walletFile = MAPPER.readValue(file, WalletFile.class);
        if (walletFile.getAddress() == null
            || walletFile.getCrypto() == null
            || walletFile.getVersion() != 3) {
          continue;
        }
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("address", walletFile.getAddress());
        entry.put("file", file.getName());
        entries.add(entry);
      } catch (Exception e) {
        // Skip files that aren't valid keystore JSON
      }
    }

    if (json) {
      try {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keystores", entries);
        System.out.println(MAPPER.writeValueAsString(result));
      } catch (Exception e) {
        System.err.println("Error writing JSON output");
        return 1;
      }
    } else if (entries.isEmpty()) {
      System.out.println("No valid keystores found in: " + keystoreDir.getAbsolutePath());
    } else {
      for (Map<String, String> entry : entries) {
        System.out.printf("%-45s %s%n", entry.get("address"), entry.get("file"));
      }
    }
    return 0;
  }

  private int printEmptyJson() {
    try {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("keystores", new ArrayList<>());
      System.out.println(MAPPER.writeValueAsString(result));
      return 0;
    } catch (Exception e) {
      System.err.println("Error writing JSON output");
      return 1;
    }
  }
}
