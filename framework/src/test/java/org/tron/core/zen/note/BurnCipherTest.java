package org.tron.core.zen.note;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.ByteUtil;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.note.NoteEncryption.Encryption;

public class BurnCipherTest {

  private static final byte[] OVK = buildTestBytes(32, 1);
  private static final byte[] NF = buildTestBytes(32, 7);
  private static final byte[] ADDR_21 = buildAddr21((byte) 0x41);

  private static byte[] buildTestBytes(int len, int seed) {
    byte[] data = new byte[len];
    for (int i = 0; i < len; i++) {
      data[i] = (byte) (i * 3 + seed);
    }
    return data;
  }

  private static byte[] buildAddr21(byte prefix) {
    byte[] addr = new byte[21];
    addr[0] = prefix;
    for (int i = 1; i < 21; i++) {
      addr[i] = (byte) (i * 2);
    }
    return addr;
  }

  private static byte[] amount32(BigInteger amount) {
    return ByteUtil.bigIntegerToBytes(amount, 32);
  }

  private static byte[] extractCipher(byte[] record) {
    return Arrays.copyOf(record, Encryption.BURN_CIPHER_LEN);
  }

  private static byte[] extractNonce(byte[] record) {
    return Arrays.copyOfRange(record,
        Encryption.BURN_NONCE_OFFSET,
        Encryption.BURN_NONCE_OFFSET + Encryption.BURN_NONCE_LEN);
  }

  private static byte[] extractReserved(byte[] record) {
    return Arrays.copyOfRange(record,
        Encryption.BURN_RESERVED_OFFSET,
        Encryption.BURN_RESERVED_OFFSET + Encryption.BURN_RESERVED_LEN);
  }

  // ---------- constants ----------

  @Test
  public void testBurnCipherSize() {
    Assert.assertEquals(80, Encryption.BURN_CIPHER_LEN);
    Assert.assertEquals(12, Encryption.BURN_NONCE_LEN);
    Assert.assertEquals(4, Encryption.BURN_RESERVED_LEN);
    Assert.assertEquals(96, Encryption.BURN_CIPHER_RECORD_SIZE);
  }

  // ---------- encrypt ----------

  @Test
  public void testEncryptProduces96ByteRecord() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(1000000);
    Optional<byte[]> recordOpt = Encryption.encryptBurnMessageByOvk(
        OVK, amount, ADDR_21, NF);
    Assert.assertTrue(recordOpt.isPresent());
    Assert.assertEquals(Encryption.BURN_CIPHER_RECORD_SIZE, recordOpt.get().length);
  }

  @Test
  public void testRecordReservedBytesCarryV2Marker() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(1000000);
    byte[] record = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    Assert.assertArrayEquals(new byte[]{0, 0, 0, 1}, extractReserved(record));
  }

  @Test
  public void testNonceEmbeddedInRecord() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(1000000);
    byte[] record = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] nonce = extractNonce(record);
    boolean allZero = true;
    for (byte b : nonce) {
      if (b != 0) {
        allZero = false;
        break;
      }
    }
    Assert.assertFalse(allZero);
  }

  @Test
  public void testNonceDeterminism() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(1000000);
    byte[] record1 = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] record2 = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    Assert.assertArrayEquals(record1, record2);
  }

  @Test
  public void testDifferentNfProducesDifferentRecord() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(1000000);
    byte[] nf2 = new byte[32];
    nf2[0] = (byte) 0xFF;

    byte[] record1 = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] record2 = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, nf2).get();
    Assert.assertFalse(Arrays.equals(record1, record2));
  }

  @Test
  public void testDifferentAmountProducesDifferentNonce() throws ZksnarkException {
    byte[] record1 = Encryption.encryptBurnMessageByOvk(
        OVK, BigInteger.valueOf(1000000), ADDR_21, NF).get();
    byte[] record2 = Encryption.encryptBurnMessageByOvk(
        OVK, BigInteger.valueOf(2000000), ADDR_21, NF).get();
    Assert.assertFalse(Arrays.equals(extractNonce(record1), extractNonce(record2)));
  }

  @Test
  public void testDifferentAddrProducesDifferentNonce() throws ZksnarkException {
    byte[] addr2 = ADDR_21.clone();
    addr2[5] ^= (byte) 0xFF;
    BigInteger amount = BigInteger.valueOf(1000000);
    byte[] record1 = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] record2 = Encryption.encryptBurnMessageByOvk(OVK, amount, addr2, NF).get();
    Assert.assertFalse(Arrays.equals(extractNonce(record1), extractNonce(record2)));
  }

  // ---------- encrypt input validation ----------

  @Test(expected = ZksnarkException.class)
  public void testEncryptRejectsNullNf() throws ZksnarkException {
    Encryption.encryptBurnMessageByOvk(OVK, BigInteger.ONE, ADDR_21, null);
  }

  @Test(expected = ZksnarkException.class)
  public void testEncryptRejectsShortOvk() throws ZksnarkException {
    Encryption.encryptBurnMessageByOvk(new byte[16], BigInteger.ONE, ADDR_21, NF);
  }

  @Test(expected = ZksnarkException.class)
  public void testEncryptRejectsBadAddrLength() throws ZksnarkException {
    Encryption.encryptBurnMessageByOvk(OVK, BigInteger.ONE, new byte[20], NF);
  }

  // ---------- decrypt round-trip ----------

  @Test
  public void testEncryptDecryptRoundTrip() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(1000000);

    byte[] record = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] cipher = extractCipher(record);
    byte[] nonce = extractNonce(record);

    Optional<byte[]> plainOpt = Encryption.decryptBurnMessageByOvk(
        OVK, cipher, nonce, extractReserved(record), NF, amount32(amount), ADDR_21);
    Assert.assertTrue(plainOpt.isPresent());
    byte[] plaintext = plainOpt.get();

    byte[] decryptedAmount = new byte[32];
    System.arraycopy(plaintext, 0, decryptedAmount, 0, 32);
    Assert.assertEquals(amount, ByteUtil.bytesToBigInteger(decryptedAmount));

    byte[] decryptedAddr = new byte[21];
    System.arraycopy(plaintext, 32, decryptedAddr, 0, 21);
    Assert.assertArrayEquals(ADDR_21, decryptedAddr);
  }

  @Test
  public void testDecryptWithWrongNfFails() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(500000);
    byte[] record = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] cipher = extractCipher(record);
    byte[] nonce = extractNonce(record);

    byte[] wrongNf = new byte[32];
    wrongNf[0] = (byte) 0xFF;
    Optional<byte[]> result = Encryption.decryptBurnMessageByOvk(
        OVK, cipher, nonce, extractReserved(record), wrongNf, amount32(amount), ADDR_21);
    Assert.assertFalse(result.isPresent());
  }

  @Test
  public void testDecryptWithWrongAmountFails() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(500000);
    byte[] record = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] cipher = extractCipher(record);
    byte[] nonce = extractNonce(record);

    Optional<byte[]> result = Encryption.decryptBurnMessageByOvk(
        OVK, cipher, nonce, extractReserved(record), NF,
        amount32(BigInteger.valueOf(500001)), ADDR_21);
    Assert.assertFalse(result.isPresent());
  }

  @Test
  public void testDecryptWithWrongAddrFails() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(500000);
    byte[] record = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] cipher = extractCipher(record);
    byte[] nonce = extractNonce(record);
    byte[] wrongAddr = ADDR_21.clone();
    wrongAddr[10] ^= (byte) 0xFF;

    Optional<byte[]> result = Encryption.decryptBurnMessageByOvk(
        OVK, cipher, nonce, extractReserved(record), NF, amount32(amount), wrongAddr);
    Assert.assertFalse(result.isPresent());
  }

  @Test
  public void testDecryptWithNullNfFailsForV2() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(500000);
    byte[] record = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] cipher = extractCipher(record);
    byte[] nonce = extractNonce(record);

    Optional<byte[]> result = Encryption.decryptBurnMessageByOvk(
        OVK, cipher, nonce, extractReserved(record), null, amount32(amount), ADDR_21);
    Assert.assertFalse(result.isPresent());
  }

  @Test
  public void testDecryptWithWrongNfLengthFailsForV2() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(500000);
    byte[] record = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] cipher = extractCipher(record);
    byte[] nonce = extractNonce(record);
    byte[] reserved = extractReserved(record);
    byte[] amt = amount32(amount);

    Assert.assertFalse(Encryption.decryptBurnMessageByOvk(
        OVK, cipher, nonce, reserved, new byte[31], amt, ADDR_21).isPresent());
    Assert.assertFalse(Encryption.decryptBurnMessageByOvk(
        OVK, cipher, nonce, reserved, new byte[33], amt, ADDR_21).isPresent());
    Assert.assertFalse(Encryption.decryptBurnMessageByOvk(
        OVK, cipher, nonce, reserved, new byte[0], amt, ADDR_21).isPresent());
  }

  @Test
  public void testDecryptWithTamperedNonceFails() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(500000);
    byte[] record = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] cipher = extractCipher(record);

    byte[] tamperedNonce = new byte[Encryption.BURN_NONCE_LEN];
    tamperedNonce[0] = (byte) 0xDE;
    Optional<byte[]> result = Encryption.decryptBurnMessageByOvk(
        OVK, cipher, tamperedNonce, extractReserved(record), NF, amount32(amount), ADDR_21);
    Assert.assertFalse(result.isPresent());
  }

  @Test
  public void testDecryptWithUnknownReservedMarkerFails() throws ZksnarkException {
    BigInteger amount = BigInteger.valueOf(500000);
    byte[] record = Encryption.encryptBurnMessageByOvk(OVK, amount, ADDR_21, NF).get();
    byte[] cipher = extractCipher(record);
    byte[] nonce = extractNonce(record);
    byte[] badReserved = new byte[]{0, 0, 0, 2};
    Optional<byte[]> result = Encryption.decryptBurnMessageByOvk(
        OVK, cipher, nonce, badReserved, NF, amount32(amount), ADDR_21);
    Assert.assertFalse(result.isPresent());
  }

  // ---------- decrypt input validation ----------

  @Test(expected = ZksnarkException.class)
  public void testDecryptRejectsNullOvk() throws ZksnarkException {
    Encryption.decryptBurnMessageByOvk(null, new byte[80], new byte[12], new byte[4], NF,
        new byte[32], ADDR_21);
  }

  @Test
  public void testDecryptRejectsBadCipherLength() throws ZksnarkException {
    Optional<byte[]> result = Encryption.decryptBurnMessageByOvk(
        OVK, new byte[64], new byte[12], new byte[4], NF, new byte[32], ADDR_21);
    Assert.assertFalse(result.isPresent());
  }

  @Test
  public void testDecryptRejectsNullNonce() throws ZksnarkException {
    Optional<byte[]> result = Encryption.decryptBurnMessageByOvk(
        OVK, new byte[80], null, new byte[4], NF, new byte[32], ADDR_21);
    Assert.assertFalse(result.isPresent());
  }

  @Test
  public void testDecryptRejectsNullReserved() throws ZksnarkException {
    Optional<byte[]> result = Encryption.decryptBurnMessageByOvk(
        OVK, new byte[80], new byte[12], null, NF, new byte[32], ADDR_21);
    Assert.assertFalse(result.isPresent());
  }
}
