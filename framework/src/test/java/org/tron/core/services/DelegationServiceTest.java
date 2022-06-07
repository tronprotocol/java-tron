package org.tron.core.services;

import static org.tron.common.utils.Commons.decodeFromBase58Check;
import static stest.tron.wallet.common.client.Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletGrpc.WalletBlockingStub;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.entity.Dec;
import org.tron.common.utils.Pair;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DecOracleRewardCapsule;
import org.tron.core.capsule.OracleRewardCapsule;
import org.tron.core.db.Manager;
import org.tron.core.service.MortgageService;
import org.tron.core.store.DelegationStore;
import org.tron.protos.Protocol.Vote;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;

@Slf4j
public class DelegationServiceTest {

  private static String fullnode = "127.0.0.1:50051";
  private MortgageService mortgageService;
  private Manager manager;

  public DelegationServiceTest(TronApplicationContext context) {
    mortgageService = context.getBean(MortgageService.class);
    manager = context.getBean(Manager.class);
  }

  public static void testGrpc() {
    WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build());
    BytesMessage.Builder builder = BytesMessage.newBuilder();
    builder.setValue(ByteString.copyFromUtf8("TLTDZBcPoJ8tZ6TTEeEqEvwYFk2wgotSfD"));
    System.out
        .println("getBrokerageInfo: " + walletStub.getBrokerageInfo(builder.build()).getNum());
    System.out.println("getRewardInfo: " + walletStub.getRewardInfo(builder.build()).getNum());
    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(
        ByteString.copyFrom(decodeFromBase58Check("TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz")))
        .setBrokerage(10);
    TransactionExtention transactionExtention = walletStub
        .updateBrokerage(updateBrokerageContract.build());
    System.out.println("UpdateBrokerage: " + transactionExtention);
  }


  private Pair<OracleRewardCapsule,DecOracleRewardCapsule> computeOracleReward(
      long cycle, AccountCapsule accountCapsule) {
    Dec balance = Dec.zeroDec();
    Map<String, Dec> asset = new HashMap<>();
    for (Vote vote : accountCapsule.getVotesList()) {
      byte[] srAddress = vote.getVoteAddress().toByteArray();
      DecOracleRewardCapsule totalReward = manager.getDelegationStore()
          .getOracleReward(cycle, srAddress);
      long totalVote = manager.getDelegationStore().getWitnessVote(cycle, srAddress);
      if (totalVote == DelegationStore.REMARK || totalVote == 0) {
        continue;
      }
      long userVote = vote.getVoteCount();
      DecOracleRewardCapsule userReward = totalReward.mul(userVote).quo(totalVote);
      balance = balance.add(userReward.getBalance());
      userReward.getAsset().forEach((k, v) -> asset.merge(k, v, Dec::add));
    }
    return new DecOracleRewardCapsule(balance, asset).truncateDecimalAndRemainder();
  }

  private void testOraclePay(int cycle) {

    int brokerage = 20;
    if (cycle == 0) {
      brokerage = 10;
    }

    byte[] sr27 = decodeFromBase58Check("TLTDZBcPoJ8tZ6TTEeEqEvwYFk2wgotSfD");
    DecOracleRewardCapsule value = manager.getDelegationStore().getOracleReward(cycle, sr27);
    Dec balance = Dec.newDec(66);
    Map<String, Dec> asset = new HashMap<>();
    asset.put("stable-01",Dec.newDec(100));
    asset.put("stable-02",Dec.newDec(25));
    asset.put("stable-03",Dec.newDec(33));
    DecOracleRewardCapsule reward = new DecOracleRewardCapsule(balance, asset);
    mortgageService.payOracleReward(sr27,new DecOracleRewardCapsule(balance, asset));
    DecOracleRewardCapsule expect = value.add(reward.mul(Dec.newDecWithPrec(100 - brokerage, 2)));
    value = manager.getDelegationStore().getOracleReward(cycle, sr27);
    Assert.assertEquals(expect.getInstance(),value.getInstance());
    manager.getDelegationStore().accumulateWitnessOracleVi(cycle, sr27,
        manager.getWitnessStore().get(sr27).getTotalShares());
  }

  private void testPay(int cycle) {
    double rate = 0.2;
    if (cycle == 0) {
      rate = 0.1;
    } else if (cycle == 1) {
      rate = 0.2;
    }
    mortgageService.payStandbyWitness();
    Wallet.setAddressPreFixByte(ADD_PRE_FIX_BYTE_MAINNET);
    byte[] sr1 = decodeFromBase58Check("TLTDZBcPoJ8tZ6TTEeEqEvwYFk2wgotSfD");
    long value = manager.getDelegationStore().getReward(cycle, sr1);
    long tmp = 0;
    for (int i = 0; i < 27; i++) {
      tmp += 100000000 + i;
    }
    double d = (double) 16000000 / tmp;
    long expect = (long) (d * 100000026);
    long brokerageAmount = (long) (rate * expect);
    expect -= brokerageAmount;
    Assert.assertEquals(expect, value);
    mortgageService.payBlockReward(sr1, 32000000);
    expect += 32000000;
    brokerageAmount = (long) (rate * 32000000);
    expect -= brokerageAmount;
    value = manager.getDelegationStore().getReward(cycle, sr1);
    Assert.assertEquals(expect, value);
  }

  private void testWithdraw() {
    //init
    manager.getDynamicPropertiesStore().saveCurrentCycleNumber(1);
    testPay(1);
    testOraclePay(1);
    manager.getDynamicPropertiesStore().saveCurrentCycleNumber(2);
    testPay(2);
    testOraclePay(2);
    byte[] sr1 = decodeFromBase58Check("THKJYuUmMKKARNf7s2VT51g5uPY6KEqnat");
    AccountCapsule accountCapsule = manager.getAccountStore().get(sr1);
    byte[] sr27 = decodeFromBase58Check("TLTDZBcPoJ8tZ6TTEeEqEvwYFk2wgotSfD");
    accountCapsule.addVotes(ByteString.copyFrom(sr27), 10000000, 10000000 * 1000_000L);
    manager.getAccountStore().put(sr1, accountCapsule);
    accountCapsule = manager.getAccountStore().get(sr1);
    //
    long allowance = accountCapsule.getAllowance();
    OracleRewardCapsule oracleAllowance = new OracleRewardCapsule(
        accountCapsule.getOracleAllowance());
    long value = mortgageService.queryReward(sr1) - allowance;
    OracleRewardCapsule retain = mortgageService.queryOracleReward(sr1)
        .sub(oracleAllowance);
    Pair<OracleRewardCapsule,DecOracleRewardCapsule> pair1 =
        computeOracleReward(0, accountCapsule);
    Pair<OracleRewardCapsule,DecOracleRewardCapsule> pair2 =
        computeOracleReward(1, accountCapsule);
    OracleRewardCapsule oracleReward1 = pair1.getKey();
    DecOracleRewardCapsule remainder1 = pair1.getValue();
    OracleRewardCapsule oracleReward2 = pair2.getKey();
    DecOracleRewardCapsule remainder2 = pair2.getValue();
    OracleRewardCapsule oracleReward = new DecOracleRewardCapsule(oracleReward1.add(oracleReward2))
        .add(remainder1).add(remainder2).truncateDecimal();
    Assert.assertEquals(retain.getInstance(), oracleReward.getInstance());
    long reward1 = (long) ((double) manager.getDelegationStore().getReward(0, sr27) / 100000000
        * 10000000);
    long reward2 = (long) ((double) manager.getDelegationStore().getReward(1, sr27) / 100000000
        * 10000000);
    long reward = reward1 + reward2;
    System.out.println("testWithdraw:" + value + ", reward:" + reward);
    Assert.assertEquals(reward, value);
    mortgageService.withdrawReward(sr1);
    accountCapsule = manager.getAccountStore().get(sr1);
    allowance = accountCapsule.getAllowance() - allowance;
    oracleAllowance = new OracleRewardCapsule(accountCapsule.getOracleAllowance())
        .sub(oracleAllowance);
    System.out.println("withdrawReward:" + allowance);
    Assert.assertEquals(reward, allowance);
    Assert.assertEquals(oracleReward.getInstance(), oracleAllowance.getInstance());
  }

  public void test() {
    manager.getDynamicPropertiesStore().saveChangeDelegation(1);
    manager.getDynamicPropertiesStore().saveAllowStableMarketOn(1);
    byte[] sr27 = decodeFromBase58Check("TLTDZBcPoJ8tZ6TTEeEqEvwYFk2wgotSfD");
    manager.getDelegationStore().setBrokerage(0, sr27, 10);
    manager.getDelegationStore().setBrokerage(1, sr27, 20);
    manager.getDelegationStore().setWitnessVote(0, sr27, 100000000);
    manager.getDelegationStore().setWitnessVote(1, sr27, 100000000);
    manager.getDelegationStore().setWitnessVote(2, sr27, 100000000);
    testPay(0);
    testOraclePay(0);
    testWithdraw();
  }
}
