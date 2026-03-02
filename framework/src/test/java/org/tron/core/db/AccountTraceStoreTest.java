package org.tron.core.db;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AccountTraceCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.AccountIndexStore;
import org.tron.core.store.AccountTraceStore;
import org.tron.protos.Protocol.AccountType;

public class AccountTraceStoreTest extends BaseTest {

  @Resource
  private AccountTraceStore accountTraceStore;
  private static byte[] address = TransactionStoreTest.randomBytes(32);

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath()
        },
        Constant.TEST_CONF
    );
  }


  @Test
  public void testRecordBalanceWithBlock() throws BadItemException, ItemNotFoundException {
    accountTraceStore.recordBalanceWithBlock(address,1,9999);
    Assert.assertNotNull(accountTraceStore.get(Bytes.concat(address,
        Longs.toByteArray(1L ^ Long.MAX_VALUE))));
  }

  @Test
  public void testGetPrevBalance() {
    accountTraceStore.recordBalanceWithBlock(address,2,9999);
    Pair<Long, Long> pair = accountTraceStore.getPrevBalance(address,2);
    Assert.assertEquals((long)pair.getKey(),2L);
    Assert.assertEquals((long)pair.getValue(), 0L);
    byte[] address2 = TransactionStoreTest.randomBytes(21);
    accountTraceStore.recordBalanceWithBlock(address2,3,99);
    Pair<Long,Long> pair2 = accountTraceStore.getPrevBalance(address2, 3);
    Assert.assertEquals((long)pair2.getKey(),3L);
    Assert.assertEquals((long)pair2.getValue(), 99L);
  }

  @Test
  public void testPut() {
    long number = 2 ^ Long.MAX_VALUE;
    long balance = 9999;
    byte[] key = Bytes.concat(address, Longs.toByteArray(number));
    accountTraceStore.put(key, new AccountTraceCapsule(balance));
    Pair<Long, Long> pair = accountTraceStore.getPrevBalance(address,2);
    Assert.assertEquals((long)pair.getKey(),2L);
    Assert.assertEquals((long)pair.getValue(), 0L);
  }

}