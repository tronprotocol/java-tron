package org.tron.consensus.base;

import com.google.protobuf.ByteString;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.args.GenesisBlock;

public class Param {

  private static volatile Param param = new Param();

  @Getter
  @Setter
  private boolean enable;
  @Getter
  @Setter
  private boolean needSyncCheck;
  @Getter
  @Setter
  private int minParticipationRate;
  @Getter
  @Setter
  private int blockProduceTimeoutPercent;
  @Getter
  @Setter
  private GenesisBlock genesisBlock;
  @Getter
  @Setter
  private List<Miner> miners;
  @Getter
  @Setter
  private BlockHandle blockHandle;
  @Getter
  @Setter
  private int agreeNodeCount;
  @Getter
  @Setter
  private PbftInterface pbftInterface;

  private Param() {

  }

  public static Param getInstance() {
    if (param == null) {
      synchronized (Param.class) {
        if (param == null) {
          param = new Param();
        }
      }
    }
    return param;
  }

  public class Miner {

    @Getter
    @Setter
    private byte[] privateKey;

    @Getter
    @Setter
    private ByteString privateKeyAddress;

    @Getter
    @Setter
    private ByteString witnessAddress;

    public Miner(byte[] privateKey, ByteString privateKeyAddress, ByteString witnessAddress) {
      this.privateKey = privateKey;
      this.privateKeyAddress = privateKeyAddress;
      this.witnessAddress = witnessAddress;
    }
  }

  public Miner getMiner() {
    return miners.get(0);
  }
}