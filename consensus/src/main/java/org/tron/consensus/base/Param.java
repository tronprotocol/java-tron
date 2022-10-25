package org.tron.consensus.base;

import com.google.protobuf.ByteString;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.args.GenesisBlock;
import org.tron.common.utils.MyAESUtil;

@Slf4j
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
  @Getter
  @Setter
  private boolean isStressTest;

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

    private String AESKey = "abcdefg111111111";

    @Setter
//    private byte[] privateKey;
    private String privateKeyStr;
    @Getter
    @Setter
    private ByteString privateKeyAddress;

    @Getter
    @Setter
    private ByteString witnessAddress;

    public Miner(byte[] privateKey, ByteString privateKeyAddress, ByteString witnessAddress) {
//      this.privateKey = privateKey;
      try {
        this.privateKeyStr = MyAESUtil.encrypt2(privateKey, AESKey);
      } catch (Exception e) {
        e.printStackTrace();
      }
      this.privateKeyAddress = privateKeyAddress;
      this.witnessAddress = witnessAddress;
    }

    public byte[] getPrivateKey() {
      try {
        return MyAESUtil.decrypt2(privateKeyStr, AESKey);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  public Miner getMiner() {
    return miners.get(0);
  }
}
