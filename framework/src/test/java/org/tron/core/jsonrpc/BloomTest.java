package org.tron.core.jsonrpc;

import static org.tron.common.bloom.Bloom.BLOOM_BYTE_SIZE;
import static org.tron.common.bloom.Bloom.getLowBits;

import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.testng.Assert;
import org.tron.common.bloom.Bloom;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;

public class BloomTest {

  @Test
  public void testGetLowBits() {
    Assert.assertEquals(getLowBits(512), 1);
    Assert.assertEquals(getLowBits(1024), 3);
    Assert.assertEquals(getLowBits(2048), 7);
    Assert.assertEquals(getLowBits(4096), 15);
  }

  @Test
  public void testBloom() {
    List<String> positive = new ArrayList<>();
    positive.add("testtest");
    positive.add("test");
    positive.add("hallo");
    positive.add("other");

    List<String> negative = new ArrayList<>();
    negative.add("tes");
    negative.add("lo");

    Bloom bloom = new Bloom();
    for (String str : positive) {
      bloom.or(Bloom.create(Hash.sha3(str.getBytes())));
    }

    for (String str : positive) {
      Assert.assertTrue(bloom.matches(Bloom.create(Hash.sha3(str.getBytes()))));
    }

    for (String str : negative) {
      if (!bloom.matches(Bloom.create(Hash.sha3(str.getBytes())))) {
        Assert.assertFalse(positive.contains(str));
      }
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
  public void benchmarkNewBloom() {
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
        .println(String.format("benchmarkNewBloom total %d times cost %d ms", times, end - start));
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

  private byte[] bytesToAddress(byte[] address) {
    byte[] data = new byte[20];
    System.arraycopy(address, 0, data, 20 - address.length, address.length);
    return data;
  }

  private TransactionInfo createTransactionInfo(byte[] address1, byte[] address2) {
    List<Log> logList = new ArrayList<>();
    List<DataWord> topics = new ArrayList<>();

    TransactionInfo.Builder builder = TransactionInfo.newBuilder();

    LogInfo logInfo =
        new LogInfo(bytesToAddress(address1), topics, new byte[0]);
    logList.add(LogInfo.buildLog(logInfo));
    logInfo =
        new LogInfo(bytesToAddress(address2), topics, new byte[0]);
    logList.add(LogInfo.buildLog(logInfo));
    builder.addAllLog(logList);

    return builder.build();

  }

  @Test
  public void benchmarkCreateByTransaction() {
    int times = 10000;

    // small
    TransactionRetCapsule smallCapsule = new TransactionRetCapsule();
    smallCapsule.addTransactionInfo(createTransactionInfo(new byte[] {0x11},
        new byte[] {0x01, 0x11}));
    smallCapsule.addTransactionInfo(createTransactionInfo(new byte[] {0x22},
        new byte[] {0x02, 0x22}));

    long start = System.currentTimeMillis();

    Bloom sBloom = new Bloom();
    for (int i = 0; i < times; i++) {
      sBloom = Bloom.createBloom(smallCapsule);
    }

    long end = System.currentTimeMillis();
    System.out.println(
        String.format("benchmarkCreateByTransaction %d times cost %d ms", times, end - start));

    String exp = "c384c56ece49458a427c67b90fefe979ebf7104795be65dc398b280f24104949";
    String got = Hex.toHexString(Hash.sha3(sBloom.getData()));
    Assert.assertEquals(got, exp);

    // large
    TransactionRetCapsule largeCapsule = new TransactionRetCapsule();
    for (int i = 0; i < 200; i++) {
      largeCapsule.addTransactionInfo(createTransactionInfo(new byte[] {0x11},
          new byte[] {0x01, 0x11}));
      largeCapsule.addTransactionInfo(createTransactionInfo(new byte[] {0x22},
          new byte[] {0x02, 0x22}));
    }

    start = System.currentTimeMillis();

    Bloom lBloom = new Bloom();
    for (int i = 0; i < times; i++) {
      lBloom = Bloom.createBloom(largeCapsule);
    }

    end = System.currentTimeMillis();
    System.out.println(
        String.format("benchmarkCreateByTransaction %d times cost %d ms", times, end - start));

    got = Hex.toHexString(Hash.sha3(lBloom.getData()));
    Assert.assertEquals(got, exp);
  }
}
