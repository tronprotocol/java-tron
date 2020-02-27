package org.tron.core.metrics;

import com.alibaba.fastjson.annotation.JSONField;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

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

//  public Protocol.MetricsInfo toProtoEntity() {
//    Protocol.MetricsInfo.Builder builder = Protocol.MetricsInfo.newBuilder();
//    builder.setStartTime(interval);
//
//    Protocol.MetricsInfo.NodeInfo.Builder nodeInfo =
//        Protocol.MetricsInfo.NodeInfo.newBuilder();
//    MetricsInfo.NodeInfo node = this.node;
//    nodeInfo.setIp(node.getIp());
//    nodeInfo.setNodeType(node.getNodeType());
//    nodeInfo.setStatus(node.getNodeType());
//    nodeInfo.setVersion(node.getVersion());
//
//    // set node info
//    builder.setNode(nodeInfo.build());
//
//    Protocol.MetricsInfo.BlockChainInfo.Builder blockChain =
//        Protocol.MetricsInfo.BlockChainInfo.newBuilder();
//    BlockchainInfo blockChainInfo = getBlockchainInfo();
//    blockChain.setHeadBlockTimestamp(blockChainInfo.getHeadBlockTimestamp());
//    blockChain.setHeadBlockHash(blockChainInfo.getHeadBlockHash());
//
//    Protocol.MetricsInfo.BlockChainInfo.TpsInfo.Builder blockProcessTime =
//        Protocol.MetricsInfo.BlockChainInfo.TpsInfo.newBuilder();
//    blockProcessTime.setMeanRate(blockChainInfo.getBlockProcessTime().getMeanRate());
//    blockProcessTime.setOneMinuteRate(blockChainInfo.getBlockProcessTime().getOneMinuteRate());
//    blockProcessTime.setFiveMinuteRate(blockChainInfo.getBlockProcessTime().getFiveMinuteRate());
//    blockProcessTime
//        .setFifteenMinuteRate(blockChainInfo.getBlockProcessTime().getFifteenMinuteRate());
//    blockChain.setBlockProcessTime(blockProcessTime.build());
//    blockChain.setSuccessForkCount(blockChainInfo.getSuccessForkCount());
//    blockChain.setFailForkCount(blockChain.getFailForkCount());
//    blockChain.setHeadBlockNum(blockChainInfo.getHeadBlockNum());
//    blockChain.setTransactionCacheSize(blockChainInfo.getTransactionCacheSize());
//    blockChain.setMissedTransactionCount(blockChainInfo.getMissedTransactionCount());
//
//    Protocol.MetricsInfo.BlockChainInfo.TpsInfo.Builder tpsInfo =
//        Protocol.MetricsInfo.BlockChainInfo.TpsInfo.newBuilder();
//    BlockchainInfo.TpsInfo tpsInfoTemp = blockChainInfo.getTps();
//    tpsInfo.setMeanRate(tpsInfoTemp.getMeanRate());
//    tpsInfo.setOneMinuteRate(tpsInfoTemp.getOneMinuteRate());
//    tpsInfo.setFiveMinuteRate(tpsInfoTemp.getFiveMinuteRate());
//    tpsInfo.setFifteenMinuteRate(tpsInfoTemp.getFifteenMinuteRate());
//    blockChain.setTps(tpsInfo.build());
//    for (BlockchainInfo.Witness witness : blockChainInfo.getWitnesses()) {
//      Protocol.MetricsInfo.BlockChainInfo.Witness.Builder witnessInfo =
//          Protocol.MetricsInfo.BlockChainInfo.Witness.newBuilder();
//      witnessInfo.setAddress(witness.getAddress());
//      witnessInfo.setVersion(witness.getVersion());
//      blockChain.addWitnesses(witnessInfo.build());
//    }
//    // set blockchain info
//    builder.setBlockchain(blockChain.build());
//
//
//    Protocol.MetricsInfo.NetInfo.Builder netInfo =
//        Protocol.MetricsInfo.NetInfo.newBuilder();
//    NetInfo netInfoTemp = getNetInfo();
//    netInfo.setConnectionCount(netInfoTemp.getConnectionCount());
//    netInfo.setValidConnectionCount(netInfoTemp.getValidConnectionCount());
//    netInfo.setErrorProtoCount(netInfoTemp.getErrorProtoCount());
//
//    Protocol.MetricsInfo.NetInfo.RateInfo.Builder tcpInTraffic =
//        Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
//    tcpInTraffic.setMeanRate(netInfoTemp.getTcpInTraffic().getMeanRate());
//    tcpInTraffic.setOneMinuteRate(netInfoTemp.getTcpInTraffic().getOneMinuteRate());
//    tcpInTraffic.setFiveMinuteRate(netInfoTemp.getTcpInTraffic().getFiveMinuteRate());
//    tcpInTraffic.setFifteenMinuteRate(netInfoTemp.getTcpInTraffic().getFifteenMinuteRate());
//    netInfo.setTcpInTraffic(tcpInTraffic.build());
//
//    Protocol.MetricsInfo.NetInfo.RateInfo.Builder tcpOutTraffic =
//        Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
//    tcpOutTraffic.setMeanRate(netInfoTemp.getTcpOutTraffic().getMeanRate());
//    tcpOutTraffic.setOneMinuteRate(netInfoTemp.getTcpOutTraffic().getOneMinuteRate());
//    tcpOutTraffic.setFiveMinuteRate(netInfoTemp.getTcpOutTraffic().getFiveMinuteRate());
//    tcpOutTraffic.setFifteenMinuteRate(netInfoTemp.getTcpOutTraffic().getFifteenMinuteRate());
//    netInfo.setTcpOutTraffic(tcpOutTraffic.build());
//
//    Protocol.MetricsInfo.NetInfo.RateInfo.Builder udpInTraffic =
//        Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
//    udpInTraffic.setMeanRate(netInfoTemp.getUdpInTraffic().getMeanRate());
//    udpInTraffic.setOneMinuteRate(netInfoTemp.getUdpInTraffic().getOneMinuteRate());
//    udpInTraffic.setFiveMinuteRate(netInfoTemp.getUdpInTraffic().getFiveMinuteRate());
//    udpInTraffic.setFifteenMinuteRate(netInfoTemp.getUdpInTraffic().getFifteenMinuteRate());
//    netInfo.setUdpInTraffic(udpInTraffic.build());
//
//    Protocol.MetricsInfo.NetInfo.RateInfo.Builder udpOutTraffic =
//        Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
//    udpOutTraffic.setMeanRate(netInfoTemp.getUdpOutTraffic().getMeanRate());
//    udpOutTraffic.setOneMinuteRate(netInfoTemp.getUdpOutTraffic().getOneMinuteRate());
//    udpOutTraffic.setFiveMinuteRate(netInfoTemp.getUdpOutTraffic().getFiveMinuteRate());
//    udpOutTraffic.setFifteenMinuteRate(netInfoTemp.getUdpOutTraffic().getFifteenMinuteRate());
//    netInfo.setUdpOutTraffic(udpOutTraffic.build());
//
//    Protocol.MetricsInfo.NetInfo.ApiInfo.common.Builder common =
//        Protocol.MetricsInfo.NetInfo.ApiInfo.common.newBuilder();
//    common.setMeanRate(netInfoTemp.getApi().getTotalCount().getMeanRate());
//    common.setOneMinute(netInfoTemp.getApi().getTotalCount().getOneMinute());
//    common.setFiveMinute(netInfoTemp.getApi().getTotalCount().getFiveMinute());
//    common.setFifteenMinute(netInfoTemp.getApi().getTotalCount().getFifteenMinute());
//    Protocol.MetricsInfo.NetInfo.ApiInfo.Builder apiInfo =
//            Protocol.MetricsInfo.NetInfo.ApiInfo.newBuilder();
//    apiInfo.setTotalCount(common.build());
//
//    common.setMeanRate(netInfoTemp.getApi().getTotalFailCount().getMeanRate());
//    common.setOneMinute(netInfoTemp.getApi().getTotalFailCount().getOneMinute());
//    common.setFiveMinute(netInfoTemp.getApi().getTotalFailCount().getFiveMinute());
//    common.setFifteenMinute(netInfoTemp.getApi().getTotalFailCount().getFifteenMinute());
//
//    apiInfo.setTotalFailCount(common.build());
//
//    common.setMeanRate(netInfoTemp.getApi().getTotalOutTraffic().getMeanRate());
//    common.setOneMinute(netInfoTemp.getApi().getTotalOutTraffic().getOneMinute());
//    common.setFiveMinute(netInfoTemp.getApi().getTotalOutTraffic().getFiveMinute());
//    common.setFifteenMinute(netInfoTemp.getApi().getTotalOutTraffic().getFifteenMinute());
//
//    apiInfo.setTotalOutTraffic(common.build());
//
//
//    for (NetInfo.ApiInfo.ApiDetailInfo apiDetailInfo : netInfoTemp.getApi().getApiDetailInfo()) {
//      Protocol.MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo.Builder apiDetail =
//          Protocol.MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo.newBuilder();
//
//      apiDetail.setName(apiDetailInfo.getName());
//      common.setMeanRate(apiDetailInfo.getCount().getMeanRate());
//      common.setOneMinute(apiDetailInfo.getCount().getOneMinute());
//      common.setFiveMinute(apiDetailInfo.getCount().getFiveMinute());
//      common.setFifteenMinute(apiDetailInfo.getCount().getFifteenMinute());
//
//      apiDetail.setCount(common.build());
//      common.setMeanRate(apiDetailInfo.getFailCount().getMeanRate());
//      common.setOneMinute(apiDetailInfo.getFailCount().getOneMinute());
//      common.setFiveMinute(apiDetailInfo.getFailCount().getFiveMinute());
//      common.setFifteenMinute(apiDetailInfo.getFailCount().getFifteenMinute());
//
//      apiDetail.setFailCount(common.build());
//
//      common.setMeanRate(apiDetailInfo.getOutTraffic().getMeanRate());
//      common.setOneMinute(apiDetailInfo.getOutTraffic().getOneMinute());
//      common.setFiveMinute(apiDetailInfo.getOutTraffic().getFiveMinute());
//      common.setFifteenMinute(apiDetailInfo.getOutTraffic().getFifteenMinute());
//      apiDetail.setOutTraffic(common.build());
//
//      apiInfo.addDetail(apiDetail.build());
//    }
//    netInfo.setApi(apiInfo.build());
//
//    netInfo.setDisconnectionCount(netInfoTemp.getDisconnectionCount());
//    for (NetInfo.DisconnectionDetailInfo disconnectionDetailInfo : netInfoTemp
//        .getDisconnectionDetail()) {
//      Protocol.MetricsInfo.NetInfo.DisconnectionDetailInfo.Builder disconnectionDetail =
//          Protocol.MetricsInfo.NetInfo.DisconnectionDetailInfo.newBuilder();
//      disconnectionDetail.setReason(disconnectionDetailInfo.getReason());
//      disconnectionDetail.setCount(disconnectionDetailInfo.getCount());
//      netInfo.addDisconnectionDetail(disconnectionDetail.build());
//    }
//
//    Protocol.MetricsInfo.NetInfo.LatencyInfo.Builder latencyInfo =
//        Protocol.MetricsInfo.NetInfo.LatencyInfo.newBuilder();
//    latencyInfo.setDelay1S(netInfoTemp.getLatency().getDelay1S());
//    latencyInfo.setDelay2S(netInfoTemp.getLatency().getDelay2S());
//    latencyInfo.setDelay3S(netInfoTemp.getLatency().getDelay3S());
//    latencyInfo.setTop99(netInfoTemp.getLatency().getTop99());
//    latencyInfo.setTop95(netInfoTemp.getLatency().getTop95());
//    latencyInfo.setTotalCount(netInfoTemp.getLatency().getTotalCount());
//
//    for (NetInfo.LatencyInfo.LatencyDetailInfo latencyDetailInfo : netInfoTemp.getLatency()
//        .getLatencyDetailInfo()) {
//      Protocol.MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo.Builder latencyDetail =
//          Protocol.MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo.newBuilder();
//      latencyDetail.setCount(latencyDetailInfo.getCount());
//      latencyDetail.setWitness(latencyDetailInfo.getWitness());
//      latencyDetail.setTop99(latencyDetailInfo.getTop99());
//      latencyDetail.setTop95(latencyDetailInfo.getTop95());
//      latencyDetail.setDelay1S(latencyDetailInfo.getDelay1S());
//      latencyDetail.setDelay2S(latencyDetailInfo.getDelay2S());
//      latencyDetail.setDelay3S(latencyDetailInfo.getDelay3S());
//      latencyInfo.addDetail(latencyDetail.build());
//    }
//
//    // set latency info
//    netInfo.setLatency(latencyInfo.build());
//    // set net info
//    builder.setNet(netInfo.build());
//    return builder.build();
//  }

  // node monitor information
  public static class NodeInfo {
    private String ip;
    private int nodeType;
    private int status;
    private String version;
    private int backupStatus;

    public int getBackupStatus() {
      return backupStatus;
    }

    public void setBackupStatus(int backupStatus) {
      this.backupStatus = backupStatus;
    }

    public String getIp() {
      return this.ip;
    }

    public NodeInfo setIp(String ip) {
      this.ip = ip;
      return this;
    }

    public int getNodeType() {
      return this.nodeType;
    }


    public NodeInfo setNodeType(int nodeType) {
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
    private TpsInfo blockProcessTime;
    private TpsInfo tps;
    private int transactionCacheSize;
    private int missedTransactionCount;
    private List<Witness> witnesses;
    private long failProcessBlockNum;
    private String failProcessBlockReason;
    private List<DupWitness> dupWitness;

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

    public TpsInfo getBlockProcessTime() {
      return this.blockProcessTime;
    }

    public BlockchainInfo setBlockProcessTime(TpsInfo blockProcessTime) {
      this.blockProcessTime = blockProcessTime;
      return this;
    }

    public TpsInfo getTps() {
      return this.tps;
    }

    public BlockchainInfo setTps(TpsInfo tps) {
      this.tps = tps;
      return this;
    }

    public int getTransactionCacheSize() {
      return this.transactionCacheSize;
    }


    public BlockchainInfo setTransactionCacheSize(int transactionCacheSize) {
      this.transactionCacheSize = transactionCacheSize;
      return this;
    }

    public int getMissedTransactionCount() {
      return this.missedTransactionCount;
    }

    public BlockchainInfo setMissedTransactionCount(int missedTransactionCount) {
      this.missedTransactionCount = missedTransactionCount;
      return this;
    }

    public List<Witness> getWitnesses() {
      return this.witnesses;
    }

    public BlockchainInfo setWitnesses(List<Witness> witnesses) {
      this.witnesses = witnesses;
      return this;
    }

    public long getFailProcessBlockNum() {
      return failProcessBlockNum;
    }

    public void setFailProcessBlockNum(long failProcessBlockNum) {
      this.failProcessBlockNum = failProcessBlockNum;
    }

    public String getFailProcessBlockReason() {
      return failProcessBlockReason;
    }

    public void setFailProcessBlockReason(String failProcessBlockReason) {
      this.failProcessBlockReason = failProcessBlockReason;
    }

    public List<DupWitness> getDupWitness() {
      return dupWitness;
    }

    public void setDupWitness(List<DupWitness> dupWitness) {
      this.dupWitness = dupWitness;
    }

    public static class TpsInfo {
      private double meanRate;
      private double oneMinuteRate;
      private double fiveMinuteRate;
      private double fifteenMinuteRate;

      public double getMeanRate() {
        return this.meanRate;
      }

      public TpsInfo setMeanRate(double meanRate) {
        this.meanRate = meanRate;
        return this;
      }

      public double getOneMinuteRate() {
        return this.oneMinuteRate;
      }

      public TpsInfo setOneMinuteRate(double oneMinuteRate) {
        this.oneMinuteRate = oneMinuteRate;
        return this;
      }

      public double getFiveMinuteRate() {
        return this.fiveMinuteRate;
      }

      public TpsInfo setFiveMinuteRate(double fiveMinuteRate) {
        this.fiveMinuteRate = fiveMinuteRate;
        return this;
      }

      public double getFifteenMinuteRate() {
        return this.fifteenMinuteRate;
      }

      public TpsInfo setFifteenMinuteRate(double fifteenMinuteRate) {
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

    public static class DupWitness {
      private String address;
      private long blockNum;
      private int count;

      public String getAddress() {
        return address;
      }

      public void setAddress(String address) {
        this.address = address;
      }

      public long getBlockNum() {
        return blockNum;
      }

      public void setBlockNum(long blockNum) {
        this.blockNum = blockNum;
      }

      public int getCount() {
        return count;
      }

      public void setCount(int count) {
        this.count = count;
      }
    }
  }

  // network monitor information
  public static class NetInfo {
    private int errorProtoCount;
    private ApiInfo api;
    private int connectionCount;
    private int validConnectionCount;
    private RateInfo tcpInTraffic;
    private RateInfo tcpOutTraffic;
    private int disconnectionCount;
    private List<DisconnectionDetailInfo> disconnectionDetail = new ArrayList<>();
    private RateInfo udpInTraffic;
    private RateInfo udpOutTraffic;
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

    public RateInfo getTcpInTraffic() {
      return this.tcpInTraffic;
    }

    @JSONField(name = "TCPInTraffic")
    public NetInfo setTcpInTraffic(RateInfo tcpInTraffic) {
      this.tcpInTraffic = tcpInTraffic;
      return this;
    }

    public RateInfo getTcpOutTraffic() {
      return this.tcpOutTraffic;
    }

    @JSONField(name = "TCPOutTraffic")
    public NetInfo setTcpOutTraffic(RateInfo tcpOutTraffic) {
      this.tcpOutTraffic = tcpOutTraffic;
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

    public RateInfo getUdpInTraffic() {
      return this.udpInTraffic;
    }

    @JSONField(name = "UDPInTraffic")
    public NetInfo setUdpInTraffic(RateInfo udpInTraffic) {
      this.udpInTraffic = udpInTraffic;
      return this;
    }

    public RateInfo getUdpOutTraffic() {
      return this.udpOutTraffic;
    }

    @JSONField(name = "UDPOutTraffic")
    public NetInfo setUdpOutTraffic(RateInfo udpOutTraffic) {
      this.udpOutTraffic = udpOutTraffic;
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
      private Common totalCount;
      private Common totalFailCount;
      private Common totalOutTraffic;
      @JSONField(name = "detail")
      private List<ApiDetailInfo> detail = new ArrayList<>();

      public Common getTotalCount() {
        return this.totalCount;
      }

      public ApiInfo setTotalCount(Common totalCount) {
        this.totalCount = totalCount;
        return this;
      }

      public Common getTotalFailCount() {
        return this.totalFailCount;
      }

      public ApiInfo setTotalFailCount(Common totalFailCount) {
        this.totalFailCount = totalFailCount;
        return this;
      }

      public Common getTotalOutTraffic() {
        return this.totalOutTraffic;
      }

      public ApiInfo setTotalOutTraffic(Common totaloutTraffic) {
        this.totalOutTraffic = totaloutTraffic;
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

      public static class Common {
        private double meanRate;
        private int oneMinute;
        private int fiveMinute;
        private int fifteenMinute;

        public double getMeanRate() {
          return this.meanRate;
        }

        public Common setMeanRate(double meanRate) {
          this.meanRate = meanRate;
          return this;
        }

        public int getOneMinute() {
          return this.oneMinute;
        }

        public Common setOneMinute(int oneMinuteCount) {
          this.oneMinute = oneMinuteCount;
          return this;
        }

        public int getFiveMinute() {
          return this.fiveMinute;
        }

        public Common setFiveMinute(int fiveMinuteCount) {
          this.fiveMinute = fiveMinuteCount;
          return this;
        }

        public int getFifteenMinute() {
          return this.fifteenMinute;
        }

        public Common setFifteenMinute(int fifteenMinuteCount) {
          this.fifteenMinute = fifteenMinuteCount;
          return this;
        }

      }

      public static class ApiDetailInfo {
        private String name;
        private Common count;
        private Common failCount;
        private Common outTraffic;

        public String getName() {
          return this.name;
        }

        public ApiDetailInfo setName(String name) {
          this.name = name;
          return this;
        }

        public Common getCount() {
          return this.count;
        }

        public ApiDetailInfo setCount(Common count) {
          this.count = count;
          return this;
        }

        public Common getFailCount() {
          return this.failCount;
        }

        public ApiDetailInfo setFailCount(Common failCount) {
          this.failCount = failCount;
          return this;
        }

        public Common getOutTraffic() {
          return this.outTraffic;
        }

        public ApiDetailInfo setOutTraffic(Common outTraffic) {
          this.outTraffic = outTraffic;
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

}
