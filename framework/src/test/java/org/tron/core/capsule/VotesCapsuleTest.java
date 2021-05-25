package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionStoreTest;
import org.tron.protos.Protocol.Vote;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class VotesCapsuleTest {

  private static String dbPath = "output_votesCapsule_test";
  private static final String OWNER_ADDRESS;
  private static List<Vote> oldVotes;
  static {
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "03702350064AD5C1A8AA6B4D74B051199CFF8EA7";
    oldVotes = new ArrayList<Vote>();

  }

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);

  }

@AfterClass
  public static void destroy(){
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
}

@Test
public void votesCapsuleTest()
{
  VotesCapsule votesCapsule = new VotesCapsule(StringUtil.hexString2ByteString(OWNER_ADDRESS),oldVotes);

  votesCapsule.addOldVotes(ByteString.copyFrom(TransactionStoreTest.randomBytes(32)),10);
  votesCapsule.addOldVotes(ByteString.copyFrom(TransactionStoreTest.randomBytes(32)),5);
  Assert.assertEquals(2, votesCapsule.getOldVotes().size());

  votesCapsule.addNewVotes(ByteString.copyFrom(TransactionStoreTest.randomBytes(32)),6);
  Assert.assertEquals(1,votesCapsule.getNewVotes().size());

  votesCapsule.clearNewVotes();
  Assert.assertTrue(votesCapsule.getNewVotes().isEmpty());

  votesCapsule.clearOldVotes();
  Assert.assertTrue(votesCapsule.getOldVotes().isEmpty());

}
}