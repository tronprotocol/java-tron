package org.tron.api;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.utils.BlockUtil;
import org.tron.model.NetworkIdentifier;
import org.tron.model.NetworkListResponse;

public class NetworkApiImplTest {

  @Test
  public void testNetworkList() {
    NetworkListResponse networkListResponse = new NetworkListResponse();
    NetworkIdentifier networkIdentifier = new NetworkIdentifier();
    networkIdentifier.setBlockchain("tron");
    networkIdentifier.setNetwork("mainnet");
    networkListResponse.addNetworkIdentifiersItem(networkIdentifier);
    String resultString = networkListResponse.toString();
    System.out.println(resultString);

    BlockCapsule genesisBlock = BlockUtil.newGenesisBlockCapsule();
    System.out.println(genesisBlock.getBlockId());
  }
}
