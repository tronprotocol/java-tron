package org.tron.core.services.monitor;

import org.springframework.stereotype.Component;
import org.tron.protos.Protocol;

@Component
public class MonitorService {
  public Protocol.MonitorInfo getDefaultInfo() {
    Protocol.MonitorInfo.Builder builder = Protocol.MonitorInfo.newBuilder();
    builder.setStatus(1);
    builder.setMsg("success");

    Protocol.MonitorInfo.DataInfo.Builder dataInfo = Protocol.MonitorInfo.DataInfo.newBuilder();
    dataInfo.setInterval(60);
    Protocol.MonitorInfo.DataInfo.NodeInfo.Builder nodeInfo  =
            Protocol.MonitorInfo.DataInfo.NodeInfo.newBuilder();
    nodeInfo.setIp("127.0.0.1");
    nodeInfo.setType(1);
    nodeInfo.setStatus(1);
    nodeInfo.setVersion("3.6.5");
    nodeInfo.setNoUpgradedSRCount(2);
    Protocol.MonitorInfo.DataInfo.NodeInfo.NoUpgradedSR.Builder noUpgradeSR =
            Protocol.MonitorInfo.DataInfo.NodeInfo.NoUpgradedSR.newBuilder();
    noUpgradeSR.setAddress("41d376d829440505ea13c9d1c455317d51b62e4ab6");
    noUpgradeSR.setUrl("http://blockchain.org");
    nodeInfo.addNoUpgradedSRList(noUpgradeSR);
    dataInfo.setNode(nodeInfo);

    Protocol.MonitorInfo.DataInfo.BlockChainInfo.Builder blockChain =
            Protocol.MonitorInfo.DataInfo.BlockChainInfo.newBuilder();
    blockChain.setHeadBlockTimestamp(1581957662);
    blockChain.setHeadBlockHash("000000000105c43e397da4a5c73cf39be735520875cf04c9d91f371103d05ec0");
    blockChain.setBlockProcessTime(1000);
    blockChain.setForkCount(1);
    blockChain.setHeadBlockNum(10000);
    blockChain.setTxQueueSize(1000);
    blockChain.setMissTx(100);
    Protocol.MonitorInfo.DataInfo.BlockChainInfo.TPSInfo.Builder tpsInfo =
            Protocol.MonitorInfo.DataInfo.BlockChainInfo.TPSInfo.newBuilder();
    tpsInfo.setMeanRate(2);
    tpsInfo.setOneMinuteRate(3);
    tpsInfo.setFiveMinuteRate(2);
    tpsInfo.setFifteenMinuteRate(4);
    blockChain.setTPS(tpsInfo);
    dataInfo.setBlockchain(blockChain);

    Protocol.MonitorInfo.DataInfo.NetInfo.Builder netInfo =
            Protocol.MonitorInfo.DataInfo.NetInfo.newBuilder();
    netInfo.setConnectionCount(20);
    netInfo.setValidConnectionCount(19);
    netInfo.setErrorProtoCount(10);
    netInfo.setTCPInTraffic(10000);
    netInfo.setTCPOutTraffic(10001);
    netInfo.setDisconnectionCount(12);
    netInfo.setUDPInTraffic(1000);
    netInfo.setUDPOutTraffic(1001);
    Protocol.MonitorInfo.DataInfo.NetInfo.ApiInfo.Builder apiInfo =
            Protocol.MonitorInfo.DataInfo.NetInfo.ApiInfo.newBuilder();
    apiInfo.setTotalCount(100);
    apiInfo.setTotalFailCount(2);
    Protocol.MonitorInfo.DataInfo.NetInfo.ApiInfo.ApiDetailInfo.Builder apiDetail =
            Protocol.MonitorInfo.DataInfo.NetInfo.ApiInfo.ApiDetailInfo.newBuilder();
    apiDetail.setName("wallet/getnodeinfo");
    apiDetail.setCount(11);
    apiDetail.setFailCount(0);
    apiInfo.addDetail(apiDetail);
    netInfo.setApi(apiInfo);

    Protocol.MonitorInfo.DataInfo.NetInfo.DisconnectionDetailInfo.Builder disconnectionDetail =
            Protocol.MonitorInfo.DataInfo.NetInfo.DisconnectionDetailInfo.newBuilder();
    disconnectionDetail.setReason("TOO_MANY_PEERS");
    disconnectionDetail.setCount(12);
    netInfo.addDisconnectionDetail(disconnectionDetail);

    Protocol.MonitorInfo.DataInfo.NetInfo.LatencyInfo.Builder latencyInfo =
            Protocol.MonitorInfo.DataInfo.NetInfo.LatencyInfo.newBuilder();
    latencyInfo.setDelay1S(12);
    latencyInfo.setDelay2S(5);
    latencyInfo.setDelay3S(1);
    latencyInfo.setTop99(10);
    latencyInfo.setTop95(6);
    latencyInfo.setTotalCount(100);
    Protocol.MonitorInfo.DataInfo.NetInfo.LatencyInfo.LatencyDetailInfo.Builder latencyDetail =
            Protocol.MonitorInfo.DataInfo.NetInfo.LatencyInfo.LatencyDetailInfo.newBuilder();
    latencyDetail.setCount(10);
    latencyDetail.setWitness("41d376d829440505ea13c9d1c455317d51b62e4ab6");
    latencyDetail.setTop99(11);
    latencyDetail.setTop95(8);
    latencyDetail.setDelay1S(3);
    latencyDetail.setDelay2S(1);
    latencyDetail.setDelay3S(0);
    latencyInfo.addDetail(latencyDetail);
    netInfo.setLatency(latencyInfo);
    dataInfo.setNet(netInfo);
    builder.setData(dataInfo);

    return builder.build();
  }
}
