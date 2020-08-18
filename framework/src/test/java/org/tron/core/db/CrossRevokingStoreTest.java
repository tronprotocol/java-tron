package org.tron.core.db;

import static org.tron.core.db.CrossRevokingStore.CHAIN_VOTED_PREFIX;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Pair;
import org.tron.core.Constant;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;

@Slf4j
public class CrossRevokingStoreTest {

  private static final String dbPath = "output-crossRevokingStore-test";
  private static TronApplicationContext context;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  private String chain1 = "chain1";
  private String chain2 = "chain2";
  private String chain3 = "chain3";
  private String chain4 = "chain4";
  private String chain5 = "chain5";
  private byte[] otherKey = "parachaininfo1".getBytes();

  private long value1 = 5;
  private long value2 = 2;
  private long value3 = 3;
  private long value4 = 4;
  private long value5 = 1;
  private byte[] otherValue = "v".getBytes();

  CrossRevokingStore crossRevokingStore;

  @Before
  public void init() {
    crossRevokingStore = context.getBean(CrossRevokingStore.class);
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testGetChainVoteCountList() {
    crossRevokingStore.updateTotalChainVote(chain1, value1);
    crossRevokingStore.updateTotalChainVote(chain4, value4);
    crossRevokingStore.updateTotalChainVote(chain2, value2);
    crossRevokingStore.updateTotalChainVote(chain5, value5);
    crossRevokingStore.updateTotalChainVote(chain3, value3);
    crossRevokingStore.put(otherKey, new BytesCapsule(otherValue));

    List<String> result = crossRevokingStore.getChainVoteCountList()
            .stream()
            .map(Pair::getKey)
            .collect(Collectors.toList());
    List<String> list = Arrays.asList(
            chain1,
            chain4,
            chain3,
            chain2,
            chain5);

    Assert.assertEquals(list.size(), result.size());
    for (int i = 0; i < result.size(); i++) {
      Assert.assertEquals(list.get(i), result.get(i));
    }
  }
}
