package stest.tron.wallet.onlineStress;

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
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.core.Wallet;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class TestApproveProposal {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key1");
  //Witness 47.93.33.201
  private final String witnessKey002 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key2");
  //Witness 123.56.10.6
  private final String witnessKey003 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key3");
  //Wtiness 39.107.80.135
  private final String witnessKey004 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key4");
  //Witness 47.93.184.2
  private final String witnessKey005 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key5");


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

  @Test(enabled = true)
  public void testApproveProposal() {
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(15L, 1L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address,witnessKey001,
        proposalMap,blockingStubFull));
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals =  Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));

    //Get proposal list after approve
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals =  Optional.ofNullable(proposalList);
    logger.info(Integer.toString(listProposals.get().getProposals(0).getApprovalsCount()));



    String[] witnessKey = {
        "22a6aca17f8ec257cc57e190902767d7fedf908bba920b4fbeaab8f158e0da17",
        "b6d8d3382c32d4d066c4f830a7e53c3da9ad8b9665dda4ca081b6cd4e807d09c",
        "03caf867c46aaf86d56aa446db80cb49305126b77bfaccfe57ab17bdb4993ccc",
        "763009595dd132aaf2d248999f2c6e7ba0acbbd9a9dfd88f7c2c158d97327645",
        "a21a3074d4d84685efaffcd7c04e3eccb541ec4c85f61c41a099cd598ad39825",
        "541a2d585fcea7e9b1803df4eb49af0eb09f1fa2ce06aa5b8ed60ac95655d66d",
        "7d5a7396d6430edb7f66aa5736ef388f2bea862c9259de8ad8c2cfe080f6f5a0",
        "7c4977817417495f4ca0c35ab3d5a25e247355d68f89f593f3fea2ab62c8644f",
        "4521c13f65cc9f5c1daa56923b8598d4015801ad28379675c64106f5f6afec30",
        "f33101ea976d90491dcb9669be568db8bbc1ad23d90be4dede094976b67d550e",
        "1bb32958909299db452d3c9bbfd15fd745160d63e4985357874ee57708435a00",
        "29c91bd8b27c807d8dc2d2991aa0fbeafe7f54f4de9fac1e1684aa57242e3922",
        "97317d4d68a0c5ce14e74ad04dfc7521f142f5c0f247b632c8f94c755bdbe669",
        "1fe1d91bbe3ac4ac5dc9866c157ef7615ec248e3fd4f7d2b49b0428da5e046b2",
        "7c37ef485e186e07952bcc8e30cd911a6cd9f2a847736c89132762fb67a42329",
        "bcc142d57d872cd2cc1235bca454f2efd5a87f612856c979cc5b45a7399272a8",
        "6054824dc03546f903a06da1f405e72409379b83395d0bbb3d4563f56e828d52",
        "87cc8832b1b4860c3c69994bbfcdae9b520e6ce40cbe2a90566e707a7e04fc70",
        "c96c92c8a5f68ffba2ced3f7cd4baa6b784838a366f62914efdc79c6c18cd7d0",
        "d29e34899a21dc801c2be88184bed29a66246b5d85f26e8c77922ee2403a1934",
        "dc51f31e4de187c1c2530d65fb8f2958dff4c37f8cea430ce98d254baae37564",
        "ff43b371d67439bb8b6fa6c4ff615c954682008343d4cb2583b19f50adbac90f",
        "dbc78781ad27f3751358333412d5edc85b13e5eee129a1a77f7232baadafae0e",
        "a79a37a3d868e66456d76b233cb894d664b75fd91861340f3843db05ab3a8c66",
        "a8107ea1c97c90cd4d84e79cd79d327def6362cc6fd498fc3d3766a6a71924f6",
        "b5076206430b2ca069ae2f4dc6f20dd0d74551559878990d1df12a723c228039",
        "442513e2e801bc42d14d33b8148851dae756d08eeb48881a44e1b2002b3fb700"
    };
    byte[] witnessAddress;
    for (String key: witnessKey) {
      witnessAddress = PublicMethed.getFinalAddress(key);
      PublicMethed.approveProposal(witnessAddress, key, proposalId,
          true, blockingStubFull);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Test(enabled = true)
  public void testGetAllNodeBlockNum() throws InterruptedException {
    String[] nodeIp = {
        "47.93.14.253:50051",
        "39.105.28.73:50051",
        "101.200.51.70:50051",
      "47.94.209.241:50051",
        "47.94.148.150:50051",
        "47.94.9.222:50051",
        "39.107.87.203:50051"
    };

    for (String ip: nodeIp) {
      fullnode = ip;
      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
      Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
      logger.info("ip " + ip + ", block num is : " + currentBlockNum);



      Integer times = 0;
      while (times++ <= -100) {
        currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
        Transaction.Contract contract;
        TransferContract transferContract;
        Integer triggerNum = 0;
        Integer transactionNum = 0;
        for (Transaction transaction: currentBlock.getTransactionsList()) {
          if (transaction.getRawData().getContract(0).getContractName().isEmpty()) {
            transactionNum++;
          } else {
            triggerNum++;

          }

        }

        logger.info("ip " + ip + ", block num is : " + currentBlockNum);
        logger.info("Transfer contract num is " + transactionNum);
        logger.info("Trigger contract num is " + triggerNum);
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }


      }


    }
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }

  @Test(enabled = true)
  public void testGetChainParameters() {
    //Set the default map
    HashMap<String, Long> defaultCommitteeMap = new HashMap<String, Long>();
    defaultCommitteeMap.put("MAINTENANCE_TIME_INTERVAL",300000L);
    defaultCommitteeMap.put("ACCOUNT_UPGRADE_COST",9999000000L);
    defaultCommitteeMap.put("CREATE_ACCOUNT_FEE",100000L);
    defaultCommitteeMap.put("TRANSACTION_FEE",10L);
    defaultCommitteeMap.put("ASSET_ISSUE_FEE",1024000000L);
    defaultCommitteeMap.put("WITNESS_PAY_PER_BLOCK",32000000L);
    defaultCommitteeMap.put("WITNESS_STANDBY_ALLOWANCE",115200000000L);
    defaultCommitteeMap.put("CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT",0L);
    defaultCommitteeMap.put("CREATE_NEW_ACCOUNT_BANDWIDTH_RATE",1L);

    ChainParameters chainParameters = blockingStubFull
        .getChainParameters(EmptyMessage.newBuilder().build());
    Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      logger.info(getChainParameters.get().getChainParameter(i).getKey());
      logger.info(Long.toString(getChainParameters.get().getChainParameter(i).getValue()));
    }

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


