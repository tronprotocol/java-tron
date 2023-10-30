package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionStoreTest;
import org.tron.protos.Protocol.Vote;


@Slf4j
public class VotesCapsuleTest {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();
  private static final String OWNER_ADDRESS;
  private static List<Vote> oldVotes;

  static {
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "03702350064AD5C1A8AA6B4D74B051199CFF8EA7";
    oldVotes = new ArrayList<Vote>();
  }

  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[]{"--output-directory",
        temporaryFolder.newFolder().toString(), "--debug"}, Constant.TEST_CONF);

  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  @Test
  public void votesCapsuleTest() {
    ByteString address = StringUtil.hexString2ByteString(OWNER_ADDRESS);
    VotesCapsule votesCapsule = new VotesCapsule(address, oldVotes);

    votesCapsule.addOldVotes(ByteString.copyFrom(TransactionStoreTest.randomBytes(32)), 10);
    votesCapsule.addOldVotes(ByteString.copyFrom(TransactionStoreTest.randomBytes(32)), 5);
    Assert.assertEquals(votesCapsule.getOldVotes().size(), 2);

    votesCapsule.addNewVotes(ByteString.copyFrom(TransactionStoreTest.randomBytes(32)), 6);
    Assert.assertEquals(votesCapsule.getNewVotes().size(), 1);

    votesCapsule.clearNewVotes();
    Assert.assertTrue(votesCapsule.getNewVotes().isEmpty());

    votesCapsule.clearOldVotes();
    Assert.assertTrue(votesCapsule.getOldVotes().isEmpty());

  }
}