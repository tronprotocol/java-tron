package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.VotesStore;
import org.tron.protos.Protocol.Vote;

@Slf4j
public class VotesStoreTest extends BaseTest {


  static {
    dbPath = "output-votesStore-test";
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
  }

  @Resource
  private VotesStore votesStore;

  @Test
  public void putAndGetVotes() {
    List<Vote> oldVotes = new ArrayList<Vote>();

    VotesCapsule votesCapsule = new VotesCapsule(ByteString.copyFromUtf8("100000000x"), oldVotes);
    this.votesStore.put(votesCapsule.createDbKey(), votesCapsule);

    Assert.assertTrue("votesStore is empyt", votesStore.iterator().hasNext());
    Assert.assertTrue(votesStore.has(votesCapsule.createDbKey()));
    VotesCapsule votesSource = this.votesStore
        .get(ByteString.copyFromUtf8("100000000x").toByteArray());
    Assert.assertEquals(votesCapsule.getAddress(), votesSource.getAddress());
    Assert.assertEquals(ByteString.copyFromUtf8("100000000x"), votesSource.getAddress());
  }
}