package stest.tron.wallet.committee;

import com.google.protobuf.ByteString;
import com.sun.media.jfxmedia.logging.Logger;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class WalletTestCommittee001 {
  //from account
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  //Witness 47.93.9.236
  private final String witnessKey001 =
      "369F095838EB6EED45D4F6312AF962D5B9DE52927DA9F04174EE49F9AF54BC77";
  //Witness 47.93.33.201
  private final String witnessKey002 =
      "9FD8E129DE181EA44C6129F727A6871440169568ADE002943EAD0E7A16D8EDAC";
  //Witness 123.56.10.6
  private final String witnessKey003 =
      "291C233A5A7660FB148BAE07FCBCF885224F2DF453239BD983F859E8E5AA4602";
  //Wtiness 39.107.80.135
  private final String witnessKey004 =
      "99676348CBF9501D07819BD4618ED885210CB5A03FEAF6BFF28F0AF8E1DE7DBE";
  //Witness 47.93.184.2
  private final String witnessKey005 =
      "FA090CFB9F3A6B00BE95FE185E82BBCFC4DA959CA6A795D275635ECF5D58466D";


  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] witness001Address = PublicMethed.getFinalAddress(witnessKey001);
  private final byte[] witness002Address = PublicMethed.getFinalAddress(witnessKey002);
  private final byte[] witness003Address = PublicMethed.getFinalAddress(witnessKey003);
  private final byte[] witness004Address = PublicMethed.getFinalAddress(witnessKey004);
  private final byte[] witness005Address = PublicMethed.getFinalAddress(witnessKey005);


  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private static final long now = System.currentTimeMillis();

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }


  @Test
  public void testListProposals() {
    //List proposals
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals =  Optional.ofNullable(proposalList);
    final Integer beforeProposalCount = listProposals.get().getProposalsCount();

    //CreateProposal
    final long now = System.currentTimeMillis();
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(0L, 1000000L);
    PublicMethed.createProposal(witness001Address,witnessKey001,proposalMap,blockingStubFull);

    //List proposals
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals =  Optional.ofNullable(proposalList);
    Integer afterProposalCount = listProposals.get().getProposalsCount();
    Assert.assertTrue(beforeProposalCount + 1 == afterProposalCount);
    logger.info(Long.toString(listProposals.get().getProposals(0).getCreateTime()));
    logger.info(Long.toString(now));
    //Assert.assertTrue(listProposals.get().getProposals(0).getCreateTime() >= now);
    Assert.assertTrue(listProposals.get().getProposals(0).getParametersMap().equals(proposalMap));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


