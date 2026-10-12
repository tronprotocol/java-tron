package org.tron.common.bloom;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.math.StrictMathWrapper;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;
import org.tron.protos.Protocol.TransactionRet;

public class BloomUtilsTest {

  @Test
  public void testHashBitsUseElevenBitsAndBigEndianBytes() {
    byte[] hash = new byte[] {(byte) 0xf8, 0, (byte) 0xf8, 8, (byte) 0xff, (byte) 0xff};
    byte[] originalHash = hash.clone();
    byte[] expected = new byte[256];
    expected[255] = 1;
    expected[254] = 1;
    expected[0] = (byte) 0x80;

    Assert.assertArrayEquals(expected, BloomUtils.create(hash));
    Assert.assertArrayEquals(expected, Bloom.create(hash).getData());
    Assert.assertArrayEquals(originalHash, hash);
  }

  @Test
  public void testCollidingBitsAndUnusedHashBytes() {
    byte[] hash = new byte[32];
    Arrays.fill(hash, (byte) 0xff);
    byte[] expected = new byte[256];
    expected[0] = (byte) 0x80;
    Assert.assertArrayEquals(expected, BloomUtils.create(hash));

    Arrays.fill(hash, 6, hash.length, (byte) 0);
    Assert.assertArrayEquals(expected, BloomUtils.create(hash));
  }

  @Test
  public void testAbsentLogsReturnNull() {
    Assert.assertNull(BloomUtils.createBloom(null));
    Assert.assertNull(BloomUtils.createBloom(TransactionRet.getDefaultInstance()));
    Assert.assertNull(BloomUtils.createBloom(TransactionRet.newBuilder()
        .addTransactioninfo(TransactionInfo.getDefaultInstance()).build()));
    Assert.assertNull(Bloom.createBloom(null));
    Assert.assertNull(Bloom.createBloom(new TransactionRetCapsule()));
  }

  @Test
  public void testMultipleTransactionsMatchIndependentEncoding() throws Exception {
    Random random = new Random(81);
    TransactionRet.Builder transactionRet = TransactionRet.newBuilder();
    for (int transaction = 0; transaction < 3; transaction++) {
      TransactionInfo.Builder info = TransactionInfo.newBuilder();
      for (int logIndex = 0; logIndex < 4; logIndex++) {
        Log.Builder log = Log.newBuilder().setAddress(randomBytes(random, 20))
            .setData(randomBytes(random, 64));
        for (int topic = 0; topic < logIndex; topic++) {
          log.addTopics(randomBytes(random, 32));
        }
        info.addLog(log);
      }
      transactionRet.addTransactioninfo(info);
    }
    TransactionRet input = transactionRet.build();
    byte[] expected = referenceBloom(input);
    Assert.assertFalse(Arrays.equals(new byte[256], expected));
    Assert.assertArrayEquals(expected, BloomUtils.createBloom(input));
    Assert.assertArrayEquals(expected,
        Bloom.createBloom(new TransactionRetCapsule(input.toByteArray())).getData());
  }

  @Test
  public void testRepeatedLogsOrderAndLogDataDoNotChangeBloom() {
    Random random = new Random(82);
    Log first = Log.newBuilder().setAddress(randomBytes(random, 20))
        .addTopics(randomBytes(random, 32)).build();
    Log second = Log.newBuilder().setAddress(randomBytes(random, 20))
        .addTopics(randomBytes(random, 32)).build();
    TransactionRet original = TransactionRet.newBuilder().addTransactioninfo(
        TransactionInfo.newBuilder().addLog(first).addLog(second)).build();
    TransactionRet repeated = TransactionRet.newBuilder().addTransactioninfo(
        TransactionInfo.newBuilder().addLog(second).addLog(first)
            .addLog(first.toBuilder().setData(randomBytes(random, 128))))
        .addTransactioninfo(TransactionInfo.getDefaultInstance()).build();

    Assert.assertArrayEquals(referenceBloom(original), BloomUtils.createBloom(repeated));
    Assert.assertArrayEquals(BloomUtils.createBloom(original), BloomUtils.createBloom(repeated));
  }

  private ByteString randomBytes(Random random, int length) {
    byte[] bytes = new byte[length];
    random.nextBytes(bytes);
    return ByteString.copyFrom(bytes);
  }

  private byte[] referenceBloom(TransactionRet transactionRet) {
    // Use an independent digest and integer bit representation to catch encoding regressions.
    BigInteger bits = BigInteger.ZERO;
    for (TransactionInfo info : transactionRet.getTransactioninfoList()) {
      for (Log log : info.getLogList()) {
        bits = addReferenceBits(bits, log.getAddress());
        for (ByteString topic : log.getTopicsList()) {
          bits = addReferenceBits(bits, topic);
        }
      }
    }
    byte[] integerBytes = bits.toByteArray();
    byte[] bloom = new byte[256];
    int length = StrictMathWrapper.min(integerBytes.length, bloom.length);
    System.arraycopy(integerBytes, integerBytes.length - length, bloom, bloom.length - length,
        length);
    return bloom;
  }

  private BigInteger addReferenceBits(BigInteger bits, ByteString value) {
    byte[] hash = new Keccak.Digest256().digest(value.toByteArray());
    for (int offset = 0; offset < 6; offset += 2) {
      int index = new BigInteger(1, Arrays.copyOfRange(hash, offset, offset + 2)).intValue() % 2048;
      bits = bits.setBit(index);
    }
    return bits;
  }
}
