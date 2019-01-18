package stest.tron.wallet.mutisign;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class WalletTestMutiSign005 {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witness001Address = PublicMethed.getFinalAddress(witnessKey001);



  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private static final long now = System.currentTimeMillis();

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[1];
  String accountPermissionJson = "";

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());



  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

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
  public void testMutiSignForProposal() {
    Assert.assertTrue(PublicMethed.sendcoin(witness001Address,10000000L,
        fromAddress,testKey002,blockingStubFull));

    ecKey1 = new ECKey(Utils.getRandom());
    manager1Address = ecKey1.getAddress();
    manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    manager2Address = ecKey2.getAddress();
    manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = witnessKey001;
    accountPermissionJson = "[{\"keys\":[{\"address\":\""
        + PublicMethed.getAddressString(witnessKey001)
        + "\",\"weight\":2}],\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\"},"
        + "{\"parent\":\"owner\",\"keys\":[{\"address\":\""
        + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},{\"address\":\""
        + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}],\"name\":\"active\","
        + "\"threshold\":2}]";
    logger.info(accountPermissionJson);
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,witness001Address,witnessKey001,
        blockingStubFull,ownerKeyString);


    //Create a proposal

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(0L, 81000L);
    Assert.assertTrue(PublicMethedForMutiSign.createProposal(witness001Address,witnessKey001,
        proposalMap,blockingStubFull,permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals =  Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));

    Assert.assertTrue(PublicMethedForMutiSign.approveProposal(witness001Address,witnessKey001,proposalId,
        true,blockingStubFull,permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Delete proposal list after approve
    Assert.assertTrue(PublicMethedForMutiSign.deleteProposal(witness001Address,witnessKey001,proposalId,blockingStubFull,permissionKeyString));

  }
  /**
   * constructor.
   */

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


