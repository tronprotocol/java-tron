package org.tron.core.services;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.service.MortgageService;
import org.tron.core.service.RewardCalService;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.WitnessStore;
import org.tron.protos.Protocol;

public class ComputeRewardTest {

  private static final byte[] OWNER_ADDRESS = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1bf0");

  private static final byte[] OWNER_ADDRESS_2 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1bf1");

  private static final byte[] OWNER_ADDRESS_3 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1bf2");

  private static final byte[] SR_ADDRESS_1 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c00");
  private static final byte[] SR_ADDRESS_2 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c01");
  private static final byte[] SR_ADDRESS_3 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c02");
  private static final byte[] SR_ADDRESS_4 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c03");
  private static final byte[] SR_ADDRESS_5 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c04");
  private static final byte[] SR_ADDRESS_6 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c05");
  private static final byte[] SR_ADDRESS_7 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c06");
  private static final byte[] SR_ADDRESS_8 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c07");
  private static final byte[] SR_ADDRESS_9 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c08");
  private static final byte[] SR_ADDRESS_10 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c09");
  private static final byte[] SR_ADDRESS_11 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c10");
  private static final byte[] SR_ADDRESS_12 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c11");
  private static final byte[] SR_ADDRESS_13 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c12");
  private static final byte[] SR_ADDRESS_14 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c13");
  private static final byte[] SR_ADDRESS_15 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c14");
  private static final byte[] SR_ADDRESS_16 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c15");
  private static final byte[] SR_ADDRESS_17 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c16");
  private static final byte[] SR_ADDRESS_18 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c17");
  private static final byte[] SR_ADDRESS_19 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c18");
  private static final byte[] SR_ADDRESS_20 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c19");
  private static final byte[] SR_ADDRESS_21 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c20");
  private static final byte[] SR_ADDRESS_22 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c21");
  private static final byte[] SR_ADDRESS_23 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c22");
  private static final byte[] SR_ADDRESS_24 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c23");
  private static final byte[] SR_ADDRESS_25 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c24");
  private static final byte[] SR_ADDRESS_26 = ByteArray.fromHexString(
      "4105b9e8af8ee371cad87317f442d155b39fbd1c25");

  private static TronApplicationContext context;
  private static DynamicPropertiesStore propertiesStore;
  private static DelegationStore delegationStore;
  private static AccountStore accountStore;
  private static RewardCalService rewardCalService;
  private static WitnessStore witnessStore;
  private static MortgageService mortgageService;
  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @AfterClass
  public static void destroy() {
    context.destroy();
    Args.clearParam();
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[]{"--output-directory", temporaryFolder.newFolder().toString(),
        "--p2p-disable", "true"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    propertiesStore = context.getBean(DynamicPropertiesStore.class);
    delegationStore = context.getBean(DelegationStore.class);
    accountStore = context.getBean(AccountStore.class);
    rewardCalService = context.getBean(RewardCalService.class);
    witnessStore = context.getBean(WitnessStore.class);
    mortgageService = context.getBean(MortgageService.class);
    setUp();
  }

  private static void setUp() {
    propertiesStore.saveChangeDelegation(1);
    propertiesStore.saveCurrentCycleNumber(4);
    propertiesStore.saveNewRewardAlgorithmEffectiveCycle();

    delegationStore.setBeginCycle(OWNER_ADDRESS, 2);
    delegationStore.setEndCycle(OWNER_ADDRESS, 3);

    delegationStore.setBeginCycle(OWNER_ADDRESS_2, 1);
    delegationStore.setEndCycle(OWNER_ADDRESS_2, 2);

    delegationStore.setBeginCycle(OWNER_ADDRESS_3, 5);
    List<Vote> votes = new ArrayList<>(32);
    votes.add(new Vote(46188095536L, 5, 1496122605L, SR_ADDRESS_1));
    votes.add(new Vote(48618386224L, 5, 1582867684L, SR_ADDRESS_2));
    votes.add(new Vote(13155856728L, 5, 586969566L, SR_ADDRESS_3));
    votes.add(new Vote(41883707392L, 5, 1342484905L, SR_ADDRESS_4));
    votes.add(new Vote(62017323832L, 5, 2061119522L, SR_ADDRESS_5));
    votes.add(new Vote(19227712L, 3, 722417L, SR_ADDRESS_6));
    votes.add(new Vote(46634987592L, 3, 1599681706L, SR_ADDRESS_7));
    votes.add(new Vote(49112700L, 3, 1753127L, SR_ADDRESS_8));
    votes.add(new Vote(40835355868L, 6, 1467015537L, SR_ADDRESS_9));
    votes.add(new Vote(10045616L, 5, 362326L, SR_ADDRESS_10));
    votes.add(new Vote(34534983616L, 5, 1217718846L, SR_ADDRESS_11));
    votes.add(new Vote(32387926028L, 5, 1292557190L, SR_ADDRESS_12));
    votes.add(new Vote(36516086396L, 5, 1295716573L, SR_ADDRESS_13));
    votes.add(new Vote(48411501224L, 5, 1575483226L, SR_ADDRESS_14));
    votes.add(new Vote(154785960L, 5, 6905922L, SR_ADDRESS_15));
    votes.add(new Vote(59057915168L, 6, 1956059729L, SR_ADDRESS_16));
    votes.add(new Vote(62921824L, 3, 2245904L, SR_ADDRESS_17));
    votes.add(new Vote(1180144L, 3, 42148L, SR_ADDRESS_18));
    votes.add(new Vote(104313216L, 5, 4654248L, SR_ADDRESS_19));
    votes.add(new Vote(20429168760L, 1, 759569195L, SR_ADDRESS_20));
    votes.add(new Vote(4706184L, 3, 168069L, SR_ADDRESS_21));
    votes.add(new Vote(55804071064L, 5, 1839919389L, SR_ADDRESS_22));
    votes.add(new Vote(6074042856L, 6, 216802459L, SR_ADDRESS_23));
    votes.add(new Vote(40729360L, 5, 1817205L, SR_ADDRESS_24));
    votes.add(new Vote(31250017036L, 5, 1242358644L, SR_ADDRESS_25));
    votes.add(new Vote(15003660L, 5, 669546L, SR_ADDRESS_26));
    Protocol.Account.Builder accountBuilder = Protocol.Account.newBuilder();
    accountBuilder.setAddress(ByteString.copyFrom(OWNER_ADDRESS));
    for (Vote vote : votes) {
      delegationStore.addReward(3, vote.srAddress, vote.totalReward);
      delegationStore.setWitnessVote(3, vote.srAddress, vote.totalVotes);
      accountBuilder.addVotes(Protocol.Vote.newBuilder()
          .setVoteAddress(ByteString.copyFrom(vote.srAddress))
          .setVoteCount(vote.userVotes));
      witnessStore.put(vote.srAddress, new WitnessCapsule(Protocol.Witness.newBuilder()
          .setAddress(ByteString.copyFrom(vote.srAddress))
          .setVoteCount(vote.totalVotes)
          .build()));
    }
    accountStore.put(OWNER_ADDRESS, new AccountCapsule(accountBuilder.build()));

    propertiesStore.saveCurrentCycleNumber(5);
  }

  @Test
  public void query() throws IOException {
    long totalReward = mortgageService.queryReward(OWNER_ADDRESS);
    rewardCalService.calRewardForTest();
    Assert.assertEquals(totalReward, mortgageService.queryReward(OWNER_ADDRESS));
  }

  static class Vote {
    long totalVotes;
    long userVotes;
    long totalReward;
    byte[] srAddress;

    public Vote(long totalReward, long userVotes, long totalVotes, byte[] srAddress) {
      this.totalVotes = totalVotes;
      this.userVotes = userVotes;
      this.totalReward = totalReward;
      this.srAddress = srAddress;
    }
  }
}
