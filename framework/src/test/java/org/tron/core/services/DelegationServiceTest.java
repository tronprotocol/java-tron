package org.tron.core.services;

import static org.tron.common.utils.Commons.decodeFromBase58Check;
import static org.tron.common.utils.client.Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.service.MortgageService;
import org.tron.core.store.WitnessStore;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class DelegationServiceTest extends BaseTest {

  // The 27 genesis SR addresses and their vote counts from config.conf (GR1..GR27).
  // GR1 has the highest votes (100000026), GR27 the lowest (100000000).
  private static final String[] SR_ADDRESSES = {
      "THKJYuUmMKKARNf7s2VT51g5uPY6KEqnat",
      "TVDmPWGYxgi5DNeW8hXrzrhY8Y6zgxPNg4",
      "TWKZN1JJPFydd5rMgMCV5aZTSiwmoksSZv",
      "TDarXEG2rAD57oa7JTK785Yb2Et32UzY32",
      "TAmFfS4Tmm8yKeoqZN8x51ASwdQBdnVizt",
      "TK6V5Pw2UWQWpySnZyCDZaAvu1y48oRgXN",
      "TGqFJPFiEqdZx52ZR4QcKHz4Zr3QXA24VL",
      "TC1ZCj9Ne3j5v3TLx5ZCDLD55MU9g3XqQW",
      "TWm3id3mrQ42guf7c4oVpYExyTYnEGy3JL",
      "TCvwc3FV3ssq2rD82rMmjhT4PVXYTsFcKV",
      "TFuC2Qge4GxA2U9abKxk1pw3YZvGM5XRir",
      "TNGoca1VHC6Y5Jd2B1VFpFEhizVk92Rz85",
      "TLCjmH6SqGK8twZ9XrBDWpBbfyvEXihhNS",
      "TEEzguTtCihbRPfjf1CvW8Euxz1kKuvtR9",
      "TZHvwiw9cehbMxrtTbmAexm9oPo4eFFvLS",
      "TGK6iAKgBmHeQyp5hn3imB71EDnFPkXiPR",
      "TLaqfGrxZ3dykAFps7M2B4gETTX1yixPgN",
      "TX3ZceVew6yLC5hWTXnjrUFtiFfUDGKGty",
      "TYednHaV9zXpnPchSywVpnseQxY9Pxw4do",
      "TCf5cqLffPccEY7hcsabiFnMfdipfyryvr",
      "TAa14iLEKPAetX49mzaxZmH6saRxcX7dT5",
      "TBYsHxDmFaRmfCF3jZNmgeJE8sDnTNKHbz",
      "TEVAq8dmSQyTYK7uP1ZnZpa6MBVR83GsV6",
      "TRKJzrZxN34YyB8aBqqPDt7g4fv6sieemz",
      "TRMP6SKeFUt5NtMLzJv8kdpYuHRnEGjGfe",
      "TDbNE1VajxjpgM5p7FyGNDASt3UVoFbiD3",
      "TLTDZBcPoJ8tZ6TTEeEqEvwYFk2wgotSfD"
  };

  @Resource
  protected MortgageService mortgageService;

  @Resource
  protected WitnessStore witnessStore;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[] {"--output-directory", dbPath(), "--debug"},
        "config-test.conf");
  }

  @Before
  public void setupWitnesses() {
    Wallet.setAddressPreFixByte(ADD_PRE_FIX_BYTE_MAINNET);
    // replace config-test.conf genesis witnesses with the 27 required ones
    witnessStore.getAllWitnesses()
        .forEach(w -> witnessStore.delete(w.getAddress().toByteArray()));
    for (int i = 0; i < SR_ADDRESSES.length; i++) {
      byte[] addr = decodeFromBase58Check(SR_ADDRESSES[i]);
      long votes = 100000026L - i;
      witnessStore.put(addr, new WitnessCapsule(ByteString.copyFrom(addr), votes, "http://GR.com"));
      // payReward calls adjustAllowance(account, brokerage) which needs the account to exist
      if (dbManager.getAccountStore().get(addr) == null) {
        dbManager.getAccountStore().put(addr,
            new AccountCapsule(ByteString.copyFrom(addr), AccountType.Normal));
      }
    }
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
