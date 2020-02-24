package org.tron.core.metrics;

import com.codahale.metrics.Meter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;

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

    public void witness(String address, String url, int version) {
      this.address = address;
      this.url = url;
      this.version = version;
    }
    public String getAddress(){
      return this.address;
    }
    public int getVersion(){
      return this.version;
    }
  }


}

