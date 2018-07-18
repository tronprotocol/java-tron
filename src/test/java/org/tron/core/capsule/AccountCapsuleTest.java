package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Vote;

public class AccountCapsuleTest {

  private static String dbPath = "output_accountCapsule_test";
  static AccountCapsule accountCapsuleTest;
  static AccountCapsule accountCapsule;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"},
        Constant.TEST_CONF);
    ByteString accountName = ByteString.copyFrom(AccountCapsuleTest.randomBytes(16));
    ByteString address = ByteString.copyFrom(AccountCapsuleTest.randomBytes(32));
    AccountType accountType = AccountType.forNumber(1);
    accountCapsuleTest = new AccountCapsule(accountName, address, accountType);
    byte[] accountByte = accountCapsuleTest.getData();
    accountCapsule = new AccountCapsule(accountByte);
    accountCapsuleTest.setBalance(1111L);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void getDataTest() {
    //test AccountCapsule onstructed function
    Assert.assertEquals(accountCapsule.getInstance().getAccountName(),
        accountCapsuleTest.getInstance().getAccountName());
    Assert.assertEquals(accountCapsule.getInstance().getType(),
        accountCapsuleTest.getInstance().getType());
    Assert.assertEquals(1111, accountCapsuleTest.getBalance());
  }

  @Test
  public void addVotesTest() {
    //test addVote and getVotesList function
    ByteString voteAddress = ByteString.copyFrom(AccountCapsuleTest.randomBytes(32));
    long voteAdd = 10L;
    accountCapsuleTest.addVotes(voteAddress, voteAdd);
    List<Vote> votesList = accountCapsuleTest.getVotesList();
    for (Vote vote :
        votesList) {
      Assert.assertEquals(voteAddress, vote.getVoteAddress());
      Assert.assertEquals(voteAdd, vote.getVoteCount());
    }
  }

  @Test
  public void AssetAmountTest() {
    //test AssetAmount ,addAsset and reduceAssetAmount function

    String nameAdd = "TokenX";
    long amountAdd = 222L;
    boolean addBoolean = accountCapsuleTest
        .addAssetAmount(nameAdd.getBytes(), amountAdd);

    Assert.assertTrue(addBoolean);

    Map<String, Long> assetMap = accountCapsuleTest.getAssetMap();
    for (Map.Entry<String, Long> entry : assetMap.entrySet()) {
      Assert.assertEquals(nameAdd, entry.getKey());
      Assert.assertEquals(amountAdd, entry.getValue().longValue());
    }
    long amountReduce = 22L;

    boolean reduceBoolean = accountCapsuleTest
        .reduceAssetAmount(ByteArray.fromString("TokenX"), amountReduce);
    Assert.assertTrue(reduceBoolean);

    Map<String, Long> assetMapAfter = accountCapsuleTest.getAssetMap();
    for (Map.Entry<String, Long> entry : assetMapAfter.entrySet()) {
      Assert.assertEquals(nameAdd, entry.getKey());
      Assert.assertEquals(amountAdd - amountReduce, entry.getValue().longValue());
    }
    String key = nameAdd;
    long value = 11L;
    boolean addAsssetBoolean = accountCapsuleTest.addAsset(key.getBytes(), value);
    Assert.assertFalse(addAsssetBoolean);

    String keyName = "TokenTest";
    long amountValue = 33L;
    boolean addAsssetTrue = accountCapsuleTest.addAsset(keyName.getBytes(), amountValue);
    Assert.assertTrue(addAsssetTrue);
  }


  public static byte[] randomBytes(int length) {
    //generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    return result;
  }
}