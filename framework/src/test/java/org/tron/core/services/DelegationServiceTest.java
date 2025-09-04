package org.tron.core.services;

import static org.tron.common.utils.Commons.decodeFromBase58Check;
import static org.tron.common.utils.client.Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.service.MortgageService;

@Slf4j
public class DelegationServiceTest extends BaseTest {

  @Resource
  protected MortgageService mortgageService;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[] {"--output-directory", dbPath(), "--debug"},
        Constant.TESTNET_CONF);
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
    long value = dbManager.getDelegationStore().getReward(cycle, sr1);
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
    value = dbManager.getDelegationStore().getReward(cycle, sr1);
    Assert.assertEquals(expect, value);
  }

  private void testWithdraw() {
    //init
    dbManager.getDynamicPropertiesStore().saveCurrentCycleNumber(1);
    testPay(1);
    dbManager.getDynamicPropertiesStore().saveCurrentCycleNumber(2);
    testPay(2);
    byte[] sr1 = decodeFromBase58Check("THKJYuUmMKKARNf7s2VT51g5uPY6KEqnat");
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(sr1);
    byte[] sr27 = decodeFromBase58Check("TLTDZBcPoJ8tZ6TTEeEqEvwYFk2wgotSfD");
    accountCapsule.addVotes(ByteString.copyFrom(sr27), 10000000);
    dbManager.getAccountStore().put(sr1, accountCapsule);
    //
    long allowance = accountCapsule.getAllowance();
    long value = mortgageService.queryReward(sr1) - allowance;
    long reward1 = (long) ((double) dbManager.getDelegationStore().getReward(0, sr27) / 100000000
        * 10000000);
    long reward2 = (long) ((double) dbManager.getDelegationStore().getReward(1, sr27) / 100000000
        * 10000000);
    long reward = reward1 + reward2;
    Assert.assertEquals(reward, value);
    mortgageService.withdrawReward(sr1);
    accountCapsule = dbManager.getAccountStore().get(sr1);
    allowance = accountCapsule.getAllowance() - allowance;
    System.out.println("withdrawReward:" + allowance);
    Assert.assertEquals(reward, allowance);
  }

  @Test
  public void test() {
    dbManager.getDynamicPropertiesStore().saveChangeDelegation(1);
    dbManager.getDynamicPropertiesStore().saveConsensusLogicOptimization(1);
    byte[] sr27 = decodeFromBase58Check("TLTDZBcPoJ8tZ6TTEeEqEvwYFk2wgotSfD");
    dbManager.getDelegationStore().setBrokerage(0, sr27, 10);
    dbManager.getDelegationStore().setBrokerage(1, sr27, 20);
    dbManager.getDelegationStore().setWitnessVote(0, sr27, 100000000);
    dbManager.getDelegationStore().setWitnessVote(1, sr27, 100000000);
    dbManager.getDelegationStore().setWitnessVote(2, sr27, 100000000);
    testPay(0);
    testWithdraw();
  }
}
