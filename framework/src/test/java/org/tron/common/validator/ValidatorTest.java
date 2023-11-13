package org.tron.common.validator;

import java.util.Random;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.RecentBlockStore;
import org.tron.core.db.TransactionCache;
import org.tron.core.db.TransactionStore;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;

@Slf4j
public class ValidatorTest extends BaseTest {

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, Constant.TEST_CONF);
  }

  @Resource
  private TransactionValidator transactionValidator;

  @Resource
  private TransactionCache transactionCache;

  @Autowired
  private TransactionStore transactionStore;

  @Autowired
  private AccountStore accountStore;

  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;

  @Autowired
  private RecentBlockStore recentBlockStore;

  @Test
  public void testDupTransactionValidator() {
    Sha256Hash hash = Sha256Hash.wrap(
        Hex.decode("1aa312142360052d64d8f5cf527b336d0b58649a1c252212f4f93d075a20deb1"));
    transactionCache.put(hash.getBytes(),  new BytesCapsule(ByteArray.fromLong(1)));
    TransactionCapsule transactionCapsule = Mockito.mock(TransactionCapsule.class);
    transactionStore.put(hash.getBytes(),  transactionCapsule);
    Mockito.when(transactionCapsule.getTransactionId()).thenReturn(hash);
    Assert.assertFalse(transactionValidator.silentValidate(transactionCapsule));
  }

  @Test
  public void testBigTransactionValidator() {
    Sha256Hash hash = Sha256Hash.wrap(
        Hex.decode("2aa312142360052d64d8f5cf527b336d0b58649a1c252212f4f93d075a20deb1"));
    TransactionCapsule transactionCapsule = Mockito.mock(TransactionCapsule.class);
    Mockito.when(transactionCapsule.getTransactionId()).thenReturn(hash);
    Mockito.when(transactionCapsule.getSerializedSize()).thenReturn(1024 * 1024 * 1024L);
    Assert.assertFalse(transactionValidator.silentValidate(transactionCapsule));
  }

  @Test
  public void testExpiredTransactionValidator() {
    Sha256Hash hash = Sha256Hash.wrap(
        Hex.decode("3aa312142360052d64d8f5cf527b336d0b58649a1c252212f4f93d075a20deb1"));
    TransactionCapsule transactionCapsule = Mockito.mock(TransactionCapsule.class);
    Mockito.when(transactionCapsule.getTransactionId()).thenReturn(hash);
    Mockito.when(transactionCapsule.getExpiration()).thenReturn(System.currentTimeMillis());
    Assert.assertFalse(transactionValidator.silentValidate(transactionCapsule));
  }

  @Test
  public void testContractSizeValidator() {
    Sha256Hash hash = Sha256Hash.wrap(
        Hex.decode("4aa312142360052d64d8f5cf527b336d0b58649a1c252212f4f93d075a20deb1"));
    TransactionCapsule tx = Mockito.mock(TransactionCapsule.class);
    Mockito.when(tx.getTransactionId()).thenReturn(hash);
    Mockito.when(tx.getSerializedSize()).thenReturn(4 * 1024L);
    Mockito.when(tx.getExpiration()).thenReturn((long) new Random().nextInt((int)
        Constant.MAXIMUM_TIME_UNTIL_EXPIRATION));
    Mockito.when(tx.getContractSize()).thenReturn(2);
    Assert.assertFalse(transactionValidator.silentValidate(tx));
  }

  @Test
  public void testTaposTransactionValidator() throws ValidateSignatureException {
    Sha256Hash hash = Sha256Hash.wrap(
        Hex.decode("5aa312142360052d64d8f5cf527b336d0b58649a1c252212f4f93d075a20deb1"));
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.wrap(
            Hex.decode("00000000035a34f46a81b39aac074b5a2ebfd71ce1685f506d6375a0c150d518")));
    TransactionCapsule tx = Mockito.mock(TransactionCapsule.class);
    Mockito.when(tx.getTransactionId()).thenReturn(hash);
    Mockito.when(tx.getSerializedSize()).thenReturn(4 * 1024L);
    Mockito.when(tx.getExpiration()).thenReturn((long) new Random().nextInt((int)
        Constant.MAXIMUM_TIME_UNTIL_EXPIRATION));
    Mockito.when(tx.getContractSize()).thenReturn(1);
    Mockito.when(tx.validateSignature(accountStore, dynamicPropertiesStore))
        .thenReturn(true);
    Mockito.when(tx.getRefBlockHash()).thenReturn(ByteArray.subArray(
        blockId.getBytes(), 8, 16));
    Mockito.when(tx.getRefBlockBytes()).thenReturn(ByteArray.subArray(
        ByteArray.fromLong(blockId.getNum()), 6, 8));
    Assert.assertFalse(transactionValidator.silentValidate(tx));
    recentBlockStore.put(ByteArray.subArray(ByteArray.fromLong(blockId.getNum()), 6, 8),
        new BytesCapsule(ByteArray.subArray(blockId.getBytes(), 8, 16)));
    Assert.assertTrue(transactionValidator.silentValidate(tx));
    recentBlockStore.put(ByteArray.subArray(ByteArray.fromLong(blockId.getNum()), 6, 8),
        new BytesCapsule(ByteArray.subArray(hash.getBytes(), 8, 16)));
    Assert.assertFalse(transactionValidator.silentValidate(tx));

  }

  @Test
  public void testSignatureValidator() throws ValidateSignatureException {
    Sha256Hash hash = Sha256Hash.wrap(
        Hex.decode("6aa312142360052d64d8f5cf527b336d0b58649a1c252212f4f93d075a20deb1"));
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.wrap(
        Hex.decode("00000000035a87d593febee134b9dc7d147eab3e9d6a40d7d2230b306c24f257")));
    TransactionCapsule tx = Mockito.mock(TransactionCapsule.class);
    Mockito.when(tx.getTransactionId()).thenReturn(hash);
    Mockito.when(tx.getSerializedSize()).thenReturn(4 * 1024L);
    Mockito.when(tx.getExpiration()).thenReturn((long) new Random().nextInt((int)
        Constant.MAXIMUM_TIME_UNTIL_EXPIRATION));
    Mockito.when(tx.getContractSize()).thenReturn(1);
    Mockito.when(tx.validateSignature(accountStore, dynamicPropertiesStore))
        .thenThrow(new ValidateSignatureException("validate signature failed"));
    recentBlockStore.put(ByteArray.subArray(ByteArray.fromLong(blockId.getNum()), 6, 8),
        new BytesCapsule(ByteArray.subArray(blockId.getBytes(), 8, 16)));
    Mockito.when(tx.getRefBlockHash()).thenReturn(ByteArray.subArray(
        blockId.getBytes(), 8, 16));
    Mockito.when(tx.getRefBlockBytes()).thenReturn(ByteArray.subArray(
        ByteArray.fromLong(blockId.getNum()), 6, 8));
    Assert.assertFalse(transactionValidator.silentValidate(tx));
  }

  @Test
  public void testSignatureValidator2() throws ValidateSignatureException {
    Sha256Hash hash = Sha256Hash.wrap(
        Hex.decode("7aa312142360052d64d8f5cf527b336d0b58649a1c252212f4f93d075a20deb1"));
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.wrap(
        Hex.decode("00000000035a87d6ec900a86650dc65dda5fde7da85952b364161d41de7ee3e2")));
    TransactionCapsule tx = Mockito.mock(TransactionCapsule.class);
    Mockito.when(tx.getTransactionId()).thenReturn(hash);
    Mockito.when(tx.getSerializedSize()).thenReturn(4 * 1024L);
    Mockito.when(tx.getExpiration()).thenReturn((long) new Random().nextInt((int)
        Constant.MAXIMUM_TIME_UNTIL_EXPIRATION));
    Mockito.when(tx.getContractSize()).thenReturn(1);
    Mockito.when(tx.validateSignature(accountStore, dynamicPropertiesStore))
        .thenThrow(new RuntimeException("validate signature failed"));
    recentBlockStore.put(ByteArray.subArray(ByteArray.fromLong(blockId.getNum()), 6, 8),
        new BytesCapsule(ByteArray.subArray(blockId.getBytes(), 8, 16)));
    Mockito.when(tx.getRefBlockHash()).thenReturn(ByteArray.subArray(
        blockId.getBytes(), 8, 16));
    Mockito.when(tx.getRefBlockBytes()).thenReturn(ByteArray.subArray(
        ByteArray.fromLong(blockId.getNum()), 6, 8));
    Assert.assertFalse(transactionValidator.silentValidate(tx));
  }
}
