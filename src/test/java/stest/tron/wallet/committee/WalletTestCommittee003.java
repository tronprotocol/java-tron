package stest.tron.wallet.committee;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class WalletTestCommittee003 {
  //from account
  private final String testKey003 =
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
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


  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
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
    Assert.assertTrue(PublicMethed.sendcoin(witness001Address,1000000L,
        toAddress,testKey003,blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(witness002Address,1000000L,
        toAddress,testKey003,blockingStubFull));
  }

  @Test(enabled = true)
  public void testApproveProposal() {
    //Create a proposal
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(0L, 81000L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address,witnessKey001,
        proposalMap,blockingStubFull));
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals =  Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));

    Assert.assertTrue(PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
        true,blockingStubFull));
        try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //Get proposal list after approve
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals =  Optional.ofNullable(proposalList);
    logger.info(Integer.toString(listProposals.get().getProposals(0).getApprovalsCount()));
    Assert.assertTrue(listProposals.get().getProposals(0).getApprovalsCount() == 1);
    //logger.info(Base58.encode58Check(witness002Address));
    //logger.info(Base58.encode58Check(listProposals.get().getProposals(0).
    // getApprovalsList().get(0).toByteArray()));
    Assert.assertTrue(Base58.encode58Check(witness002Address).equals(Base58.encode58Check(
        listProposals.get().getProposals(0).getApprovalsList().get(0).toByteArray())));

    //Failed to approve proposal when you already approval this proposal
    Assert.assertFalse(PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
        true,blockingStubFull));

    //Success to change the option from true to false.
    Assert.assertTrue(PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
        false,blockingStubFull));
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals =  Optional.ofNullable(proposalList);
    Assert.assertTrue(listProposals.get().getProposals(0).getApprovalsCount() == 0);

    //Failed to approvel proposal when you already approval this proposal
    Assert.assertFalse(PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
        false,blockingStubFull));


    //Non witness can't approval proposal
    Assert.assertFalse(PublicMethed.approveProposal(toAddress,testKey003,proposalId,
        true,blockingStubFull));

    //Muti approval
    Assert.assertTrue(PublicMethed.approveProposal(witness001Address,witnessKey001,proposalId,
        true,blockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
        true,blockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness003Address,witnessKey003,proposalId,
        true,blockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness004Address,witnessKey004,proposalId,
        true,blockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness005Address,witnessKey005,proposalId,
        true,blockingStubFull));
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals =  Optional.ofNullable(proposalList);
    Assert.assertTrue(listProposals.get().getProposals(0).getApprovalsCount() == 5);


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


