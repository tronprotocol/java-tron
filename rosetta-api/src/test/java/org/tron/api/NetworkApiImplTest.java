package org.tron.api;

import org.junit.Test;
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
  }
}
