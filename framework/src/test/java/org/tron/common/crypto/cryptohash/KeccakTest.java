package org.tron.common.crypto.cryptohash;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.tron.common.crypto.jce.TronCastleProvider;

@RunWith(Parameterized.class)
public class KeccakTest {

  private final int bits;

  public KeccakTest(int bits) {
    this.bits = bits;
  }

  @Parameterized.Parameters(name = "Keccak{0}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][]{{256}, {512}});
  }

  @Test
  public void testByteArrayDigest() throws Exception {
    byte[] input = inputBytes();
    assertArrayEquals(referenceDigest(input), newDigest().digest(input));
  }

  @Test
  public void testHeapByteBuffer() throws Exception {
    checkBuffer(ByteBuffer.wrap(inputBytes()));
  }

  @Test
  public void testDirectByteBuffer() throws Exception {
    ByteBuffer buffer = ByteBuffer.allocateDirect(inputBytes().length);
    buffer.put(inputBytes());
    buffer.flip();
    checkBuffer(buffer);
  }

  @Test
  public void testReadOnlyByteBuffer() throws Exception {
    checkBuffer(ByteBuffer.wrap(inputBytes()).asReadOnlyBuffer());
  }

  @Test
  public void testByteBufferSlice() throws Exception {
    byte[] input = inputBytes();
    ByteBuffer buffer = ByteBuffer.allocate(input.length + 10);
    buffer.position(5);
    buffer.put(input);
    buffer.limit(5 + input.length);
    buffer.position(5);
    checkBuffer(buffer.slice());
  }

  @Test
  public void testByteBufferWindow() throws Exception {
    ByteBuffer buffer = ByteBuffer.wrap(inputBytes());
    buffer.position(7);
    buffer.limit(buffer.limit() - 11);
    checkBuffer(buffer);
  }

  @Test
  public void testEmptyByteBuffer() throws Exception {
    ByteBuffer buffer = ByteBuffer.wrap(inputBytes());
    buffer.position(buffer.limit());
    checkBuffer(buffer);
  }

  @Test
  public void testMixedInput() throws Exception {
    byte[] input = inputBytes();
    MessageDigest digest = newDigest();
    digest.update(input[0]);
    digest.update(input, 1, 16);
    ByteBuffer buffer = ByteBuffer.wrap(input, 17, input.length - 17);
    digest.update(buffer);
    assertEquals(buffer.limit(), buffer.position());
    assertArrayEquals(referenceDigest(input), digest.digest());
  }

  @Test
  public void testInvalidUpdateRangePreservesState() throws Exception {
    byte[] input = inputBytes();
    byte[] invalidInput = Arrays.copyOf(input, 200);
    int[][] ranges = {
        {-1, 1}, {-1, 0}, {0, -1}, {invalidInput.length + 1, 0},
        {invalidInput.length, 1}, {0, 300},
        {Integer.MAX_VALUE, 1}, {1, Integer.MAX_VALUE}
    };
    for (int prefixLength : new int[]{0, 17, 200}) {
      for (int[] range : ranges) {
        MessageDigest digest = newDigest();
        digest.update(input, 0, prefixLength);

        assertThrows(IllegalArgumentException.class,
            () -> digest.update(invalidInput, range[0], range[1]));

        digest.update(input, prefixLength, input.length - prefixLength);
        assertArrayEquals(referenceDigest(input), digest.digest());
      }
    }
  }

  @Test
  public void testNullUpdatePreservesState() throws Exception {
    byte[] input = inputBytes();
    for (int length : new int[]{0, 1, -1}) {
      MessageDigest digest = newDigest();
      digest.update(input, 0, 17);

      assertThrows(IllegalArgumentException.class, () -> digest.update(null, 0, length));

      digest.update(input, 17, input.length - 17);
      assertArrayEquals(referenceDigest(input), digest.digest());
    }
  }

  @Test
  public void testEmptyUpdateRangePreservesState() throws Exception {
    byte[] input = inputBytes();
    MessageDigest digest = newDigest();
    digest.update(input, 0, 17);

    digest.update(input, input.length, 0);
    digest.update(new byte[0], 0, 0);

    digest.update(input, 17, input.length - 17);
    assertArrayEquals(referenceDigest(input), digest.digest());
  }

  @Test
  public void testDigestOutputRange() throws Exception {
    byte[] input = inputBytes();
    byte[] expected = referenceDigest(input);
    for (int available : new int[]{expected.length, expected.length + 5}) {
      MessageDigest digest = newDigest();
      digest.update(input);
      byte[] output = new byte[expected.length + 12];
      Arrays.fill(output, (byte) 0x5a);
      byte[] expectedOutput = output.clone();
      System.arraycopy(expected, 0, expectedOutput, 3, expected.length);
      assertEquals(expected.length, digest.digest(output, 3, available));
      assertArrayEquals(expectedOutput, output);
      assertArrayEquals(referenceDigest(new byte[0]), digest.digest());
    }
  }

  @Test
  public void testShortDigestPreservesState() throws Exception {
    byte[] input = inputBytes();
    for (int available : new int[]{0, 1, bits / 8 - 1}) {
      for (int capacity : new int[]{available, bits / 8}) {
        MessageDigest digest = newDigest();
        digest.update(input, 0, 17);
        byte[] output = new byte[capacity];
        Arrays.fill(output, (byte) 0x5a);
        byte[] original = output.clone();
        assertThrows(DigestException.class, () -> digest.digest(output, 0, available));
        assertArrayEquals(original, output);
        digest.update(input, 17, input.length - 17);
        assertArrayEquals(referenceDigest(input), digest.digest());
      }
    }
  }

  @Test
  public void testInvalidDigestRangePreservesState() throws Exception {
    byte[] input = inputBytes();
    int length = bits / 8;
    int[][] ranges = {
        {-1, length}, {0, -1}, {length + 1, 0}, {1, length},
        {Integer.MAX_VALUE, length}, {1, Integer.MAX_VALUE}
    };
    for (int[] range : ranges) {
      MessageDigest digest = newDigest();
      digest.update(input);
      byte[] output = new byte[length];
      Arrays.fill(output, (byte) 0x5a);
      byte[] original = output.clone();
      assertThrows(IllegalArgumentException.class,
          () -> digest.digest(output, range[0], range[1]));
      assertArrayEquals(original, output);
      assertArrayEquals(referenceDigest(input), digest.digest());
    }
    MessageDigest digest = newDigest();
    digest.update(input);
    assertThrows(IllegalArgumentException.class, () -> digest.digest(null, 0, length));
    assertArrayEquals(referenceDigest(input), digest.digest());
  }

  @Test
  public void testDigestInterface() throws Exception {
    Digest digest = (Digest) newDigest();
    byte[] input = inputBytes();
    digest.update(input);
    assertThrows(DigestException.class, () -> digest.digest(new byte[1], 0, 1));
    byte[] output = new byte[bits / 8];
    assertEquals(output.length, digest.digest(output, 0, output.length));
    assertArrayEquals(referenceDigest(input), output);
  }

  @Test
  public void testSpiMethods() throws Exception {
    byte[] input = inputBytes();
    byte[] actual;
    byte[] afterReset;
    if (bits == 256) {
      Keccak256 digest = (Keccak256) newDigest();
      digest.engineUpdate(input[0]);
      digest.engineUpdate(input, 1, input.length - 1);
      actual = digest.engineDigest();
      afterReset = digest.engineDigest();
    } else {
      Keccak512 digest = (Keccak512) newDigest();
      digest.engineUpdate(input[0]);
      digest.engineUpdate(input, 1, input.length - 1);
      actual = digest.engineDigest();
      afterReset = digest.engineDigest();
    }
    assertArrayEquals(referenceDigest(input), actual);
    assertArrayEquals(referenceDigest(new byte[0]), afterReset);
  }

  @Test
  public void testShortSpiDigestPreservesState() throws Exception {
    for (int inputLength : new int[]{17, 8193}) {
      byte[] input = Arrays.copyOf(inputBytes(), inputLength);
      byte[] expected = referenceDigest(input);
      for (int available : new int[]{0, 1, expected.length - 1}) {
        DigestEngine digest = bits == 256 ? new TestKeccak256() : new TestKeccak512();
        digest.update(input);
        byte[] output = new byte[expected.length + 6];
        Arrays.fill(output, (byte) 0x5a);
        byte[] expectedOutput = output.clone();

        assertThrows(DigestException.class, () -> spiDigest(digest, output, 3, available));
        assertArrayEquals(expectedOutput, output);

        assertEquals(expected.length, spiDigest(digest, output, 3, expected.length));
        System.arraycopy(expected, 0, expectedOutput, 3, expected.length);
        assertArrayEquals(expectedOutput, output);
        assertArrayEquals(referenceDigest(new byte[0]), digest.digest());
      }
    }
  }

  @Test
  public void testSpiResetClearsBufferedState() throws Exception {
    DigestEngine digest = (DigestEngine) newDigest();
    int blockLength = digest.getBlockLength();
    for (int length : new int[]{0, 1, blockLength - 1, blockLength, blockLength + 1,
        2 * blockLength + 3}) {
      byte[] input = Arrays.copyOf(inputBytes(), length);
      if (bits == 256) {
        ((Keccak256) digest).engineUpdate(input, 0, input.length);
      } else {
        ((Keccak512) digest).engineUpdate(input, 0, input.length);
      }
      digest.engineReset();
      assertEquals("Buffered bytes after reset for length " + length, 0, digest.flush());
      assertEquals("Processed blocks after reset for length " + length, 0, digest.getBlockCount());
      byte[] afterReset = bits == 256
          ? ((Keccak256) digest).engineDigest() : ((Keccak512) digest).engineDigest();
      assertArrayEquals(referenceDigest(new byte[0]), afterReset);
    }
  }

  @Test
  public void testSpiResetBeforeNewInput() throws Exception {
    DigestEngine digest = (DigestEngine) newDigest();
    digest.update(inputBytes());
    digest.engineReset();
    digest.engineReset();
    byte[] nextInput = {1, 2, 3};
    digest.update(nextInput);
    assertArrayEquals(referenceDigest(nextInput), digest.digest());
  }

  @Test
  public void testCopy() throws Exception {
    DigestEngine digest = (DigestEngine) newDigest();
    byte[] input = inputBytes();
    digest.update(ByteBuffer.wrap(input, 0, 151));
    Digest copy = digest.copy();
    digest.update(ByteBuffer.wrap(input, 151, input.length - 151));
    assertArrayEquals(referenceDigest(input), digest.digest());
    assertArrayEquals(referenceDigest(Arrays.copyOf(input, 151)), copy.digest());
  }

  private MessageDigest newDigest() throws Exception {
    return MessageDigest.getInstance("TRON-KECCAK-" + bits, TronCastleProvider.getInstance());
  }

  private int spiDigest(DigestEngine digest, byte[] output, int offset, int len)
      throws DigestException {
    return bits == 256
        ? ((TestKeccak256) digest).engineDigest(output, offset, len)
        : ((TestKeccak512) digest).engineDigest(output, offset, len);
  }

  private static class TestKeccak256 extends Keccak256 {

    @Override
    protected int engineDigest(byte[] output, int offset, int len) throws DigestException {
      return super.engineDigest(output, offset, len);
    }
  }

  private static class TestKeccak512 extends Keccak512 {

    @Override
    protected int engineDigest(byte[] output, int offset, int len) throws DigestException {
      return super.engineDigest(output, offset, len);
    }
  }

  private byte[] referenceDigest(byte[] input) {
    KeccakDigest digest = new KeccakDigest(bits);
    digest.update(input, 0, input.length);
    byte[] output = new byte[bits / 8];
    digest.doFinal(output, 0);
    return output;
  }

  private byte[] inputBytes() {
    byte[] input = new byte[8193];
    for (int i = 0; i < input.length; i++) {
      input[i] = (byte) i;
    }
    return input;
  }

  private void checkBuffer(ByteBuffer buffer) throws Exception {
    byte[] input = new byte[buffer.remaining()];
    buffer.duplicate().get(input);
    int limit = buffer.limit();
    MessageDigest digest = newDigest();
    digest.update(buffer);
    assertEquals(limit, buffer.position());
    assertEquals(limit, buffer.limit());
    assertArrayEquals(referenceDigest(input), digest.digest());
    assertArrayEquals(referenceDigest(new byte[0]), digest.digest());
  }
}
