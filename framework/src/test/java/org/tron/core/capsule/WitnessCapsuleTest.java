package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;


public class WitnessCapsuleTest {

  private static String dbPath = "output_witnessCapsule_test";
  private static final String OWNER_ADDRESS;

  static {
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "03702350064AD5C1A8AA6B4D74B051199CFF8EA7";
  }

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);

  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void witnessShareTest() {
    WitnessCapsule witnessCapsule = new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 10_000L, "");
    Assert.assertEquals(10_000_000_000L, witnessCapsule.getTotalShares());
    Assert.assertEquals(100_000_000L, witnessCapsule.sharesFromVoteCount(100));
    Assert.assertEquals(100, witnessCapsule.voteCountFromShares(100_000_000L));
    witnessCapsule.setTotalShares(10_000_000_000L);
    Assert.assertEquals(100_000_000L, witnessCapsule.sharesFromVoteCount(100));
    Assert.assertEquals(100, witnessCapsule.voteCountFromShares(100_000_000L));
    witnessCapsule.setVoteCount(9999);
    Assert.assertEquals(1_000_100L, witnessCapsule.sharesFromVoteCount(1));
    Assert.assertEquals(1, witnessCapsule.voteCountFromShares(1_000_100L));
  }

  @Test
  public void witnessShareTest2() {
    WitnessCapsule witnessCapsule = new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 100, "");
    Assert.assertEquals(100_000_000L, witnessCapsule.getTotalShares());
    Assert.assertEquals(100_000_000L, witnessCapsule.sharesFromVoteCount(100));
    Assert.assertEquals(100, witnessCapsule.voteCountFromShares(100_000_000L));
    witnessCapsule.setVoteCount(99);
    witnessCapsule.setTotalShares(100_000_000L);
    Assert.assertEquals(1_010_101L, witnessCapsule.sharesFromVoteCount(1));
    Assert.assertEquals(1, witnessCapsule.voteCountFromShares(1_010_101L));
    Assert.assertEquals(1_010_101_010L, witnessCapsule.sharesFromVoteCount(1000));
    Assert.assertEquals(1000, witnessCapsule.voteCountFromShares(1_010_101_010L));
  }
}
