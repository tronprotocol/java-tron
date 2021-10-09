package org.tron.core.jsonrpc;

import static org.tron.common.bloom.Bloom.BLOOM_BYTE_SIZE;
import static org.tron.common.bloom.Bloom.getLowBits;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.testng.Assert;
import org.tron.common.bloom.Bloom;
import org.tron.common.crypto.Hash;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.TransactionRetCapsule;

public class BloomTest {

  @Test
  public void testGetLowBits() {
    Assert.assertEquals(getLowBits(512), 1);
    Assert.assertEquals(getLowBits(1024), 3);
    Assert.assertEquals(getLowBits(2048), 7);
    Assert.assertEquals(getLowBits(4096), 15);
  }

  @Test
  public void testBloomMatches() {
    String[] positive = new String[]{"testtest", "test", "hallo", "other"};
    String[] negative = new String[]{"tes", "lo"};

    Bloom bloom = new Bloom();
    for (String str : positive) {
      bloom.or(Bloom.create(Hash.sha3(str.getBytes())));
    }

    for (String str : positive) {
      Assert.assertTrue(bloom.matches(Bloom.create(Hash.sha3(str.getBytes()))));
    }

    for (String str: negative) {
      Assert.assertFalse(bloom.matches(Bloom.create(Hash.sha3(str.getBytes()))));
    }
  }

  @Test
  public void testBloomExtensively() {
    String exp = "c8d3ca65cdb4874300a9e39475508f23ed6da09fdbc487f89a2dcf50b09eb263";
    Bloom b = new Bloom();

    for (int i = 0; i < 100; i++) {
      String data = String.format("xxxxxxxxxx data %d yyyyyyyyyyyyyy", i);
      b.or(Bloom.create(Hash.sha3(data.getBytes())));
    }
    String got = Hex.toHexString(Hash.sha3(b.getData()));

    Assert.assertEquals(got, exp);

    Bloom b2 = new Bloom(b.getData());
    Assert.assertEquals(Hex.toHexString(Hash.sha3(b2.getData())), exp);
  }

  @Test
  public void benchmarkBloom() {
    int times = 100000;
    byte[] data = new byte[BLOOM_BYTE_SIZE];
    byte[] test = "testestestest".getBytes();
    System.arraycopy(test, 0, data, 0, test.length);

    long start = System.currentTimeMillis();

    for (int i = 0; i < times; i++) {
      Bloom bloom = new Bloom(data);
    }

    long end = System.currentTimeMillis();
    System.out
        .println(String.format("benchmarkBloom total %d times cost %d ms", times, end - start));
  }

  @Test
  public void benchmarkMatches() {
    int times = 100000;
    byte[] test = "testtest".getBytes();

    long start = System.currentTimeMillis();
    Bloom bloom = new Bloom();
    for (int i = 0; i < times; i++) {
      bloom.matches(Bloom.create(Hash.sha3(test)));
    }

    long end = System.currentTimeMillis();
    System.out.println(
        String.format("benchmarkMatches total %d times cost %d ms", times, end - start));
  }

  @Test
  public void benchmarkCreateByTransaction() {
    // TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();
    // transactionInfoCapsule.setContractAddress(new byte[] {0x01, 0x11, 0x11});
    // // transactionInfoCapsule.addAllLog();
    //
    // // transactionInfoCapsule.setId(transactionId);
    // transactionInfoCapsule.setFee(1000L);
    // transactionInfoCapsule.setBlockNumber(100L);
    // transactionInfoCapsule.setBlockTimeStamp(200L);
    //
    // TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule();
    // transactionRetCapsule.addTransactionInfo(transactionInfoCapsule.getInstance());

  }
}
