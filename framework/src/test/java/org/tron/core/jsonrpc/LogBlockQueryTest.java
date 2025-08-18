package org.tron.core.jsonrpc;

import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.filters.LogBlockQuery;
import org.tron.core.services.jsonrpc.filters.LogFilterWrapper;
import org.tron.core.store.SectionBloomStore;

public class LogBlockQueryTest extends BaseTest {

  @Resource
  SectionBloomStore sectionBloomStore;
  private ExecutorService sectionExecutor;
  private Method partialMatchMethod;
  private static final long CURRENT_MAX_BLOCK_NUM = 50000L;

  static {
    Args.setParam(new String[] {"--output-directory", dbPath()}, Constant.TEST_CONF);
  }

  @Before
  public void setup() throws Exception {
    sectionExecutor = Executors.newFixedThreadPool(5);
    
    // Get private method through reflection
    partialMatchMethod = LogBlockQuery.class.getDeclaredMethod("partialMatch", 
        int[][].class, int.class);
    partialMatchMethod.setAccessible(true);

    BitSet bitSet = new BitSet(SectionBloomStore.BLOCK_PER_SECTION);
    bitSet.set(0, SectionBloomStore.BLOCK_PER_SECTION);
    sectionBloomStore.put(0, 1, bitSet);
    sectionBloomStore.put(0, 2, bitSet);
    sectionBloomStore.put(0, 3, bitSet);
    BitSet bitSet2 = new BitSet(SectionBloomStore.BLOCK_PER_SECTION);
    bitSet2.set(0);
    sectionBloomStore.put(1, 1, bitSet2);
    sectionBloomStore.put(1, 2, bitSet2);
    sectionBloomStore.put(1, 3, bitSet2);
  }

  @Test
  public void testPartialMatch() throws Exception {
    // Create a basic LogFilterWrapper
    LogFilterWrapper logFilterWrapper = new LogFilterWrapper(
        new FilterRequest("0x0", "0x1", null, null, null),
        CURRENT_MAX_BLOCK_NUM, null, false);
    
    LogBlockQuery logBlockQuery = new LogBlockQuery(logFilterWrapper, sectionBloomStore, 
        CURRENT_MAX_BLOCK_NUM, sectionExecutor);

    int section = 0;

    // Create a hit condition
    int[][] bitIndexes = new int[][] {
        {1, 2, 3},  // topic0
        {4, 5, 6}   // topic1
    };

    // topic0 hit section 0
    BitSet result = (BitSet) partialMatchMethod.invoke(logBlockQuery, bitIndexes, section);
    Assert.assertNotNull(result);
    Assert.assertEquals(SectionBloomStore.BLOCK_PER_SECTION, result.cardinality());

    // topic0 hit section 1
    result = (BitSet) partialMatchMethod.invoke(logBlockQuery, bitIndexes, 1);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.cardinality());

    // not exist section 2
    result = (BitSet) partialMatchMethod.invoke(logBlockQuery, bitIndexes, 2);
    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());

    //not hit
    bitIndexes = new int[][] {
        {1, 2, 4},  // topic0
        {3, 5, 6}   // topic1
    };
    result = (BitSet) partialMatchMethod.invoke(logBlockQuery, bitIndexes, section);
    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());

    // null condition
    bitIndexes = new int[0][];
    result = (BitSet) partialMatchMethod.invoke(logBlockQuery, bitIndexes, section);
    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }
} 