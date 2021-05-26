package org.tron.core.services.jsonrpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.services.NodeInfoService;

@Component
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