package org.tron.core.metrics.blockchain;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "blockChainInfo")
public class BlockChainInfo {
  public static long startRecordTime;

  public static class Witness {
    private String address;
    private String url;
    private int version;

    public Witness(String address, int version) {
      this.address = address;
      this.version = version;
    }

    public Witness(String address, String url, int version) {
      this.address = address;
      this.url = url;
      this.version = version;
    }

    public String getAddress() {
      return this.address;
    }

    public int getVersion() {
      return this.version;
    }

    public Witness setVersion(int version) {
      this.version = version;
      return this;
    }
  }

}

