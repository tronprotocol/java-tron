package org.tron.core.metrics;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol;

@Slf4j
public class MetricsInfo {
  private int interval;

  @JSONField(name = "node")
  private NodeInfo node;

  @JSONField(name = "blockchain")
  private BlockchainInfo blockchain;

  @JSONField(name = "net")
  private NetInfo net;

  public int getInterval() {
    return this.interval;
  }


  public MetricsInfo setInterval(int interval) {
    this.interval = interval;
    return this;
  }

  @JSONField(name = "node")
  public NodeInfo getNodeInfo() {
    return this.node;
  }


  public MetricsInfo setNodeInfo(NodeInfo node) {
    this.node = node;
    return this;
  }

  public MetricsInfo setBlockInfo(BlockchainInfo blockchain) {
    this.blockchain = blockchain;
    return this;
  }

  @JSONField(name = "blockchain")
  public BlockchainInfo getBlockchainInfo() {
    return this.blockchain;
  }

  @JSONField(name = "net")
  public NetInfo getNetInfo() {
    return this.net;
  }


  public MetricsInfo setNetInfo(NetInfo net) {
    this.net = net;
    return this;
  }

  // node monitor information
  public static class NodeInfo {
    private String ip;
    @JSONField(name = "nodeType")
    private int nodeType;
    private int status;
    private String version;

    public String getIp() {
      return this.ip;
    }

    public NodeInfo setIp(String ip) {
      this.ip = ip;
      return this;
    }

    @JSONField(name = "Type")
    public int getType() {
      return this.nodeType;
    }


    public NodeInfo setType(int nodeType) {
      this.nodeType = nodeType;
      return this;
    }

    public int getStatus() {
      return this.status;
    }

    public NodeInfo setStatus(int status) {
      this.status = status;
      return this;
    }

    public String getVersion() {
      return this.version;
    }

    public NodeInfo setVersion(String version) {
      this.version = version;
      return this;
    }

  }

  // blockchain monitor information
  public static class BlockchainInfo {
    private int headBlockNum;
    private long headBlockTimestamp;
    private String headBlockHash;
    private int successForkCount;
    private int failForkCount;
    private TPSInfo blockProcessTime;
    @JSONField(name = "TPS")
    private TPSInfo TPS;
    @JSONField(name = "TxCacheSize")
    private int TxCacheSize;
    private int missTxCount;
    @JSONField(name = "witnesses")
    private List<Witness> witnesses;

    public int getHeadBlockNum() {
      return this.headBlockNum;
    }

    public BlockchainInfo setHeadBlockNum(int headBlockNum) {
      this.headBlockNum = headBlockNum;
      return this;
    }

    public long getHeadBlockTimestamp() {
      return this.headBlockTimestamp;
    }

    public BlockchainInfo setHeadBlockTimestamp(long headBlockTimestamp) {
      this.headBlockTimestamp = headBlockTimestamp;
      return this;
    }

    public String getHeadBlockHash() {
      return this.headBlockHash;
    }

    public BlockchainInfo setHeadBlockHash(String headBlockHash) {
      this.headBlockHash = headBlockHash;
      return this;
    }

    public int getSuccessForkCount() {
      return this.successForkCount;
    }

    public BlockchainInfo setSuccessForkCount(int forkCount) {
      this.successForkCount = forkCount;
      return this;
    }

    public int getFailForkCount() {
      return this.failForkCount;
    }

    public BlockchainInfo setFailForkCount(int forkCount) {
      this.failForkCount = forkCount;
      return this;
    }

    public TPSInfo getBlockProcessTime() {
      return this.blockProcessTime;
    }

    public BlockchainInfo setBlockProcessTime(TPSInfo blockProcessTime) {
      this.blockProcessTime = blockProcessTime;
      return this;
    }

    @JSONField(name = "TPS")
    public TPSInfo getTPS() {
      return this.TPS;
    }

    public BlockchainInfo setTPS(TPSInfo TPS) {
      this.TPS = TPS;
      return this;
    }

    @JSONField(name = "TxCacheSize")
    public int getTxCacheSize() {
      return this.TxCacheSize;
    }


    public BlockchainInfo setTxCacheSize(int TxCacheSize) {
      this.TxCacheSize = TxCacheSize;
      return this;
    }

    public int getMissTxCount() {
      return this.missTxCount;
    }

    public BlockchainInfo setMissTxCount(int missTxCount) {
      this.missTxCount = missTxCount;
      return this;
    }

    public List<Witness> getWitnesses() {
      return this.witnesses;
    }

    public BlockchainInfo setWitnesses(List<Witness> witnesses) {
      this.witnesses = witnesses;
      return this;
    }

    public static class TPSInfo {
      private double meanRate;
      private double oneMinuteRate;
      private double fiveMinuteRate;
      private double fifteenMinuteRate;

      public double getMeanRate() {
        return this.meanRate;
      }

      public TPSInfo setMeanRate(double meanRate) {
        this.meanRate = meanRate;
        return this;
      }

      public double getOneMinuteRate() {
        return this.oneMinuteRate;
      }

      public TPSInfo setOneMinuteRate(double oneMinuteRate) {
        this.oneMinuteRate = oneMinuteRate;
        return this;
      }

      public double getFiveMinuteRate() {
        return this.fiveMinuteRate;
      }

      public TPSInfo setFiveMinuteRate(double fiveMinuteRate) {
        this.fiveMinuteRate = fiveMinuteRate;
        return this;
      }

      public double getFifteenMinuteRate() {
        return this.fifteenMinuteRate;
      }

      public TPSInfo setFifteenMinuteRate(double fifteenMinuteRate) {
        this.fifteenMinuteRate = fifteenMinuteRate;
        return this;
      }

    }

    public static class Witness {
      private String address;
      private int version;

      public String getAddress() {
        return this.address;
      }

      public Witness setAddress(String address) {
        this.address = address;
        return this;
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

  // network monitor information
  public static class NetInfo {
    private int errorProtoCount;
    private ApiInfo api;
    private int connectionCount;
    private int validConnectionCount;
    @JSONField(name = "TCPInTraffic")
    private RateInfo TCPInTraffic;
    @JSONField(name = "TCPOutTraffic")
    private RateInfo TCPOutTraffic;
    private int disconnectionCount;
    private List<DisconnectionDetailInfo> disconnectionDetail = new ArrayList<>();
    @JSONField(name = "UDPInTraffic")
    private RateInfo UDPInTraffic;
    @JSONField(name = "UDPOutTraffic")
    private RateInfo UDPOutTraffic;
    private LatencyInfo latency;

    public int getErrorProtoCount() {
      return this.errorProtoCount;
    }

    public NetInfo setErrorProtoCount(int errorProtoCount) {
      this.errorProtoCount = errorProtoCount;
      return this;
    }

    public ApiInfo getApi() {
      return this.api;
    }

    public NetInfo setApi(ApiInfo api) {
      this.api = api;
      return this;
    }

    public int getConnectionCount() {
      return this.connectionCount;
    }

    public NetInfo setConnectionCount(int connectionCount) {
      this.connectionCount = connectionCount;
      return this;
    }

    public int getValidConnectionCount() {
      return this.validConnectionCount;
    }

    public NetInfo setValidConnectionCount(int validConnectionCount) {
      this.validConnectionCount = validConnectionCount;
      return this;
    }

    public RateInfo getTCPInTraffic() {
      return this.TCPInTraffic;
    }

    @JSONField(name = "TCPInTraffic")
    public NetInfo setTCPInTraffic(RateInfo TCPInTraffic) {
      this.TCPInTraffic = TCPInTraffic;
      return this;
    }

    public RateInfo getTCPOutTraffic() {
      return this.TCPOutTraffic;
    }

    @JSONField(name = "TCPOutTraffic")
    public NetInfo setTCPOutTraffic(RateInfo TCPOutTraffic) {
      this.TCPOutTraffic = TCPOutTraffic;
      return this;
    }

    public int getDisconnectionCount() {
      return this.disconnectionCount;
    }

    public NetInfo setDisconnectionCount(int disconnectionCount) {
      this.disconnectionCount = disconnectionCount;
      return this;
    }

    public List<DisconnectionDetailInfo> getDisconnectionDetail() {
      return this.disconnectionDetail;
    }

    public NetInfo setDisconnectionDetail(List<DisconnectionDetailInfo> disconnectionDetail) {
      this.disconnectionDetail = disconnectionDetail;
      return this;
    }

    public RateInfo getUDPInTraffic() {
      return this.UDPInTraffic;
    }

    @JSONField(name = "UDPInTraffic")
    public NetInfo setUDPInTraffic(RateInfo UDPInTraffic) {
      this.UDPInTraffic = UDPInTraffic;
      return this;
    }

    public RateInfo getUDPOutTraffic() {
      return this.UDPOutTraffic;
    }

    @JSONField(name = "UDPOutTraffic")
    public NetInfo setUDPOutTraffic(RateInfo UDPOutTraffic) {
      this.UDPOutTraffic = UDPOutTraffic;
      return this;
    }

    public LatencyInfo getLatency() {
      return this.latency;
    }

    public NetInfo setLatency(LatencyInfo latency) {
      this.latency = latency;
      return this;
    }

    // API monitor information
    public static class ApiInfo {
      private int totalCount;
      private int totalFailCount;
      @JSONField(name = "detail")
      private List<ApiDetailInfo> detail = new ArrayList<>();

      public int getTotalCount() {
        return this.totalCount;
      }

      public ApiInfo setTotalCount(int totalCount) {
        this.totalCount = totalCount;
        return this;
      }

      public int getTotalFailCount() {
        return this.totalFailCount;
      }

      public ApiInfo setTotalFailCount(int totalFailCount) {
        this.totalFailCount = totalFailCount;
        return this;
      }

      @JSONField(name = "detail")
      public List<ApiDetailInfo> getApiDetailInfo() {
        return this.detail;
      }

      public ApiInfo setApiDetailInfo(List<ApiDetailInfo> detail) {
        this.detail = detail;
        return this;
      }

      public static class ApiDetailInfo {
        private String name;
        private int count;
        private int failCount;

        public String getName() {
          return this.name;
        }

        public ApiDetailInfo setName(String name) {
          this.name = name;
          return this;
        }

        public int getCount() {
          return this.count;
        }

        public ApiDetailInfo setCount(int count) {
          this.count = count;
          return this;
        }

        public int getFailCount() {
          return this.failCount;
        }

        public ApiDetailInfo setFailCount(int failCount) {
          this.failCount = failCount;
          return this;
        }
      }
    }

    // disconnection monitor information
    public static class DisconnectionDetailInfo {
      private String reason;
      private int count;

      public String getReason() {
        return this.reason;
      }

      public DisconnectionDetailInfo setReason(String reason) {
        this.reason = reason;
        return this;
      }

      public int getCount() {
        return this.count;
      }

      public DisconnectionDetailInfo setCount(int count) {
        this.count = count;
        return this;
      }

    }

    // latency monitor information
    public static class LatencyInfo {
      private int top99;
      private int top95;
      private int totalCount;
      private int delay1S;
      private int delay2S;
      private int delay3S;
      @JSONField(name = "detail")
      private List<LatencyDetailInfo> detail = new ArrayList<>();

      public int getTop99() {
        return this.top99;
      }

      public LatencyInfo setTop99(int top99) {
        this.top99 = top99;
        return this;
      }

      public int getTop95() {
        return this.top95;
      }

      public LatencyInfo setTop95(int top95) {
        this.top95 = top95;
        return this;
      }

      public int getTotalCount() {
        return this.totalCount;
      }

      public LatencyInfo setTotalCount(int totalCount) {
        this.totalCount = totalCount;
        return this;
      }

      public int getDelay1S() {
        return this.delay1S;
      }

      public LatencyInfo setDelay1S(int delay1S) {
        this.delay1S = delay1S;
        return this;
      }

      public int getDelay2S() {
        return this.delay2S;
      }

      public LatencyInfo setDelay2S(int delay2S) {
        this.delay2S = delay2S;
        return this;
      }

      public int getDelay3S() {
        return this.delay3S;
      }

      public LatencyInfo setDelay3S(int delay3S) {
        this.delay3S = delay3S;
        return this;
      }

      @JSONField(name = "detail")
      public List<LatencyDetailInfo> getLatencyDetailInfo() {
        return this.detail;
      }

      public LatencyInfo setLatencyDetailInfo(List<LatencyDetailInfo> detail) {
        this.detail = detail;
        return this;
      }

      public static class LatencyDetailInfo {
        private String witness;
        private int top99;
        private int top95;
        private int count;
        private int delay1S;
        private int delay2S;
        private int delay3S;

        public String getWitness() {
          return this.witness;
        }

        public LatencyDetailInfo setWitness(String witness) {
          this.witness = witness;
          return this;
        }

        public int getTop99() {
          return this.top99;
        }

        public LatencyDetailInfo setTop99(int top99) {
          this.top99 = top99;
          return this;
        }

        public int getTop95() {
          return this.top95;
        }

        public LatencyDetailInfo setTop95(int top95) {
          this.top95 = top95;
          return this;
        }

        public int getCount() {
          return this.count;
        }

        public LatencyDetailInfo setCount(int count) {
          this.count = count;
          return this;
        }

        public int getDelay1S() {
          return this.delay1S;
        }

        public LatencyDetailInfo setDelay1S(int delay1S) {
          this.delay1S = delay1S;
          return this;
        }

        public int getDelay2S() {
          return this.delay2S;
        }

        public LatencyDetailInfo setDelay2S(int delay2S) {
          this.delay2S = delay2S;
          return this;
        }

        public int getDelay3S() {
          return this.delay3S;
        }

        public LatencyDetailInfo setDelay3S(int delay3S) {
          this.delay3S = delay3S;
          return this;
        }

      }
    }

    public static class RateInfo {
      private double meanRate;
      private double oneMinuteRate;
      private double fiveMinuteRate;
      private double fifteenMinuteRate;

      public double getMeanRate() {
        return this.meanRate;
      }

      public RateInfo setMeanRate(double meanRate) {
        this.meanRate = meanRate;
        return this;
      }

      public double getOneMinuteRate() {
        return this.oneMinuteRate;
      }

      public RateInfo setOneMinuteRate(double oneMinuteRate) {
        this.oneMinuteRate = oneMinuteRate;
        return this;
      }

      public double getFiveMinuteRate() {
        return this.fiveMinuteRate;
      }

      public RateInfo setFiveMinuteRate(double fiveMinuteRate) {
        this.fiveMinuteRate = fiveMinuteRate;
        return this;
      }

      public double getFifteenMinuteRate() {
        return this.fifteenMinuteRate;
      }

      public RateInfo setFifteenMinuteRate(double fifteenMinuteRate) {
        this.fifteenMinuteRate = fifteenMinuteRate;
        return this;
      }

    }
  }

  public Protocol.MetricsInfo ToProtoEntity() {
    Protocol.MetricsInfo.Builder builder = Protocol.MetricsInfo.newBuilder();
    builder.setStartTime(interval);

    Protocol.MetricsInfo.NodeInfo.Builder nodeInfo =
            Protocol.MetricsInfo.NodeInfo.newBuilder();
    MetricsInfo.NodeInfo node = this.node;
    nodeInfo.setIp(node.getIp());
    nodeInfo.setNodeType(node.getType());
    nodeInfo.setStatus(node.getType());
    nodeInfo.setVersion(node.getVersion());

    // set node info
    builder.setNode(nodeInfo.build());

    Protocol.MetricsInfo.BlockChainInfo.Builder blockChain =
            Protocol.MetricsInfo.BlockChainInfo.newBuilder();
    BlockchainInfo blockChainInfo = getBlockchainInfo();
    blockChain.setHeadBlockTimestamp(blockChainInfo.getHeadBlockTimestamp());
    blockChain.setHeadBlockHash(blockChainInfo.getHeadBlockHash());

    Protocol.MetricsInfo.BlockChainInfo.TPSInfo.Builder blockProcessTime =
            Protocol.MetricsInfo.BlockChainInfo.TPSInfo.newBuilder();
    blockProcessTime.setMeanRate(blockChainInfo.getBlockProcessTime().getMeanRate());
    blockProcessTime.setOneMinuteRate(blockChainInfo.getBlockProcessTime().getOneMinuteRate());
    blockProcessTime.setFiveMinuteRate(blockChainInfo.getBlockProcessTime().getFiveMinuteRate());
    blockProcessTime
        .setFifteenMinuteRate(blockChainInfo.getBlockProcessTime().getFifteenMinuteRate());
    blockChain.setBlockProcessTime(blockProcessTime.build());
    blockChain.setSuccessForkCount(blockChainInfo.getSuccessForkCount());
    blockChain.setFailForkCount(blockChain.getFailForkCount());
    blockChain.setHeadBlockNum(blockChainInfo.getHeadBlockNum());
    blockChain.setTxCacheSize(blockChainInfo.getTxCacheSize());
    blockChain.setMissedTxCount(blockChainInfo.getMissTxCount());

    Protocol.MetricsInfo.BlockChainInfo.TPSInfo.Builder tpsInfo =
            Protocol.MetricsInfo.BlockChainInfo.TPSInfo.newBuilder();
    BlockchainInfo.TPSInfo TpsInfo = blockChainInfo.getTPS();
    tpsInfo.setMeanRate(TpsInfo.getMeanRate());
    tpsInfo.setOneMinuteRate(TpsInfo.getOneMinuteRate());
    tpsInfo.setFiveMinuteRate(TpsInfo.getFiveMinuteRate());
    tpsInfo.setFifteenMinuteRate(TpsInfo.getFifteenMinuteRate());
    blockChain.setTPS(tpsInfo.build());
    for (BlockchainInfo.Witness witness : blockChainInfo.getWitnesses()) {
      Protocol.MetricsInfo.BlockChainInfo.Witness.Builder witnessInfo =
              Protocol.MetricsInfo.BlockChainInfo.Witness.newBuilder();
      witnessInfo.setAddress(witness.getAddress());
      witnessInfo.setVersion(witness.getVersion());
      blockChain.addWitnesses(witnessInfo.build());
    }
    // set blockchain info
    builder.setBlockchain(blockChain.build());


    Protocol.MetricsInfo.NetInfo.Builder netInfo =
            Protocol.MetricsInfo.NetInfo.newBuilder();
    NetInfo netInfoTemp = getNetInfo();
    netInfo.setConnectionCount(netInfoTemp.getConnectionCount());
    netInfo.setValidConnectionCount(netInfoTemp.getValidConnectionCount());
    netInfo.setErrorProtoCount(netInfoTemp.getErrorProtoCount());

    Protocol.MetricsInfo.NetInfo.RateInfo.Builder tcpInTraffic =
            Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
    tcpInTraffic.setMeanRate(netInfoTemp.getTCPInTraffic().getMeanRate());
    tcpInTraffic.setOneMinuteRate(netInfoTemp.getTCPInTraffic().getOneMinuteRate());
    tcpInTraffic.setFiveMinuteRate(netInfoTemp.getTCPInTraffic().getFiveMinuteRate());
    tcpInTraffic.setFifteenMinuteRate(netInfoTemp.getTCPInTraffic().getFifteenMinuteRate());
    netInfo.setTCPInTraffic(tcpInTraffic);

    Protocol.MetricsInfo.NetInfo.RateInfo.Builder tcpOutTraffic =
            Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
    tcpOutTraffic.setMeanRate(netInfoTemp.getTCPOutTraffic().getMeanRate());
    tcpOutTraffic.setOneMinuteRate(netInfoTemp.getTCPOutTraffic().getOneMinuteRate());
    tcpOutTraffic.setFiveMinuteRate(netInfoTemp.getTCPOutTraffic().getFiveMinuteRate());
    tcpOutTraffic.setFifteenMinuteRate(netInfoTemp.getTCPOutTraffic().getFifteenMinuteRate());
    netInfo.setTCPOutTraffic(tcpOutTraffic);

    Protocol.MetricsInfo.NetInfo.RateInfo.Builder udpInTraffic =
            Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
    udpInTraffic.setMeanRate(netInfoTemp.getUDPInTraffic().getMeanRate());
    udpInTraffic.setOneMinuteRate(netInfoTemp.getUDPInTraffic().getOneMinuteRate());
    udpInTraffic.setFiveMinuteRate(netInfoTemp.getUDPInTraffic().getFiveMinuteRate());
    udpInTraffic.setFifteenMinuteRate(netInfoTemp.getUDPInTraffic().getFifteenMinuteRate());
    netInfo.setUDPInTraffic(udpInTraffic);

    Protocol.MetricsInfo.NetInfo.RateInfo.Builder udpOutTraffic =
            Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
    udpOutTraffic.setMeanRate(netInfoTemp.getUDPOutTraffic().getMeanRate());
    udpOutTraffic.setOneMinuteRate(netInfoTemp.getUDPOutTraffic().getOneMinuteRate());
    udpOutTraffic.setFiveMinuteRate(netInfoTemp.getUDPOutTraffic().getFiveMinuteRate());
    udpOutTraffic.setFifteenMinuteRate(netInfoTemp.getUDPOutTraffic().getFifteenMinuteRate());
    netInfo.setUDPOutTraffic(udpOutTraffic);

    Protocol.MetricsInfo.NetInfo.ApiInfo.Builder apiInfo =
            Protocol.MetricsInfo.NetInfo.ApiInfo.newBuilder();
    apiInfo.setTotalCount(netInfoTemp.getApi().getTotalCount());
    apiInfo.setTotalFailCount(netInfoTemp.getApi().getTotalFailCount());
    for (NetInfo.ApiInfo.ApiDetailInfo ApiDetail : netInfoTemp.getApi().getApiDetailInfo()) {
      Protocol.MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo.Builder apiDetail =
              Protocol.MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo.newBuilder();
      apiDetail.setName(ApiDetail.getName());
      apiDetail.setCount(ApiDetail.getCount());
      apiDetail.setFailCount(ApiDetail.getFailCount());
      apiInfo.addDetail(apiDetail.build());
    }
    netInfo.setApi(apiInfo.build());

    netInfo.setDisconnectionCount(netInfoTemp.getDisconnectionCount());
    for (NetInfo.DisconnectionDetailInfo DisconnectionDetail : netInfoTemp
            .getDisconnectionDetail()) {
      Protocol.MetricsInfo.NetInfo.DisconnectionDetailInfo.Builder disconnectionDetail =
              Protocol.MetricsInfo.NetInfo.DisconnectionDetailInfo.newBuilder();
      disconnectionDetail.setReason(DisconnectionDetail.getReason());
      disconnectionDetail.setCount(DisconnectionDetail.getCount());
      netInfo.addDisconnectionDetail(disconnectionDetail.build());
    }

    Protocol.MetricsInfo.NetInfo.LatencyInfo.Builder latencyInfo =
            Protocol.MetricsInfo.NetInfo.LatencyInfo.newBuilder();
    latencyInfo.setDelay1S(netInfoTemp.getLatency().getDelay1S());
    latencyInfo.setDelay2S(netInfoTemp.getLatency().getDelay2S());
    latencyInfo.setDelay3S(netInfoTemp.getLatency().getDelay3S());
    latencyInfo.setTop99(netInfoTemp.getLatency().getTop99());
    latencyInfo.setTop95(netInfoTemp.getLatency().getTop95());
    latencyInfo.setTotalCount(netInfoTemp.getLatency().getTotalCount());

    for (NetInfo.LatencyInfo.LatencyDetailInfo LatencyDetailInfo : netInfoTemp.getLatency()
            .getLatencyDetailInfo()) {
      Protocol.MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo.Builder latencyDetail =
              Protocol.MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo.newBuilder();
      latencyDetail.setCount(LatencyDetailInfo.getCount());
      latencyDetail.setWitness(LatencyDetailInfo.getWitness());
      latencyDetail.setTop99(LatencyDetailInfo.getTop99());
      latencyDetail.setTop95(LatencyDetailInfo.getTop95());
      latencyDetail.setDelay1S(LatencyDetailInfo.getDelay1S());
      latencyDetail.setDelay2S(LatencyDetailInfo.getDelay2S());
      latencyDetail.setDelay3S(LatencyDetailInfo.getDelay3S());
      latencyInfo.addDetail(latencyDetail.build());
    }

    // set latency info
    netInfo.setLatency(latencyInfo.build());
    // set net info
    builder.setNet(netInfo.build());
    return builder.build();
  }

}
