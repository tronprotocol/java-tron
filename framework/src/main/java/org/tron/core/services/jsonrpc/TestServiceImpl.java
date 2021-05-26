package org.tron.core.services.jsonrpc;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.program.Version;

import org.tron.core.Wallet;
import org.tron.core.services.NodeInfoService;

public class TestServiceImpl implements TestService {

  private NodeInfoService nodeInfoService;
  private Wallet wallet;

  public TestServiceImpl() {
  }

  public TestServiceImpl(NodeInfoService nodeInfoService, Wallet wallet) {
    this.nodeInfoService = nodeInfoService;
    this.wallet = wallet;
  }

  @Override
  public int getInt(int code) {
    return code;
  }

  public String web3ClientVersion() {
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

  public String web3Sha3(String data) {
    byte[] result = Hash.sha3(ByteArray.fromHexString(data));
    return ByteArray.toJsonHex(result);
  }

  @Override
  public int getNetVersion() {
    //当前链的id，不能跟metamask已有的id冲突
    return 100;
  }

  @Override
  public boolean isListening() {
    int activeConnectCount = nodeInfoService.getNodeInfo().getActiveConnectCount();
    return activeConnectCount >= 1;
  }

  @Override
  public int getProtocolVersion() {
    //当前块的版本号。实际是与代码版本对应的。
    return wallet.getNowBlock().getBlockHeader().getRawData().getVersion();
  }
}