package org.tron.core.services.jsonrpc;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.program.Version;

public class TestServiceImpl implements TestService {
  public int getInt(int code) {
    return code;
  }

  public String web3_clientVersion() {
    Pattern shortVersion = Pattern.compile("(\\d\\.\\d).*");
    Matcher matcher = shortVersion.matcher(System.getProperty("java.version"));
    matcher.matches();

    return Arrays.asList(
        "TRON", "v" + Version.getVersion(),
        System.getProperty("os.name"),
        "Java" + matcher.group(1),
        Version.VERSION_NAME).stream()
        .collect(Collectors.joining("/"));
  }

  public String web3_sha3(String data) {
    byte[] result = Hash.sha3(ByteArray.fromHexString(data));
    return ByteArray.toJsonHex(result);
  }
}