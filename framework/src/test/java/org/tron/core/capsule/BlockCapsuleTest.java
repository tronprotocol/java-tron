package org.tron.core.capsule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;

@Slf4j
public class BlockCapsuleTest {

  private final String privateKey = PublicMethod.getRandomPrivateKey();
  private LocalWitnesses localWitnesses;

  private static BlockCapsule blockCapsule0 = new BlockCapsule(1,
      Sha256Hash.wrap(ByteString
          .copyFrom(ByteArray
              .fromHexString("9938a342238077182498b464ac0292229938a342238077182498b464ac029222"))),
      1234,
      ByteString.copyFrom("1234567".getBytes()));
  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
  }

  @Test
  public void testCalcMerkleRoot() throws Exception {
    blockCapsule0.setMerkleRoot();
    Assert.assertEquals(
        Sha256Hash.wrap(Sha256Hash.ZERO_HASH.getByteString()).toString(),
        blockCapsule0.getMerkleRoot().toString());

    logger.info("Transaction[X] Merkle Root : {}", blockCapsule0.getMerkleRoot().toString());

    TransferContract transferContract1 = TransferContract.newBuilder()
        .setAmount(1L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086"))))
        .build();

    TransferContract transferContract2 = TransferContract.newBuilder()
        .setAmount(2L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "ED738B3A0FE390EAA71B768B6D02CDBD18FB207B"))))
        .build();

    blockCapsule0
        .addTransaction(new TransactionCapsule(transferContract1, ContractType.TransferContract));
    blockCapsule0
        .addTransaction(new TransactionCapsule(transferContract2, ContractType.TransferContract));
    blockCapsule0.setMerkleRoot();

    Assert.assertEquals(
        "5bc862243292e6aa1d5e21a60bb6a673e4c2544709f6363d4a2f85ec29bcfe00",
        blockCapsule0.getMerkleRoot().toString());

    logger.info("Transaction[O] Merkle Root : {}", blockCapsule0.getMerkleRoot().toString());
  }

  @Test
  public void testValidateMerkleRoot() throws Exception {
    // build a fresh local block so shared blockCapsule0 is not mutated
    String parentHash = "9938a342238077182498b464ac0292229938a342238077182498b464ac029222";
    BlockCapsule local = new BlockCapsule(1,
        Sha256Hash.wrap(ByteString.copyFrom(ByteArray.fromHexString(parentHash))),
        1234,
        ByteString.copyFrom("1234567".getBytes()));

    // valid block: setMerkleRoot then validate — should not throw
    local.setMerkleRoot();
    local.validateMerkleRoot(); // no exception

    // flag is set — second call must be a no-op (no recomputation)
    local.validateMerkleRoot(); // still no exception

    // tamper with a transaction to break merkle
    TransferContract transferContract = TransferContract.newBuilder()
        .setAmount(999L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086")))
        .build();
    local.addTransaction(
        new TransactionCapsule(transferContract, ContractType.TransferContract));
    // merkle root was set before adding the tx, so it is now stale/invalid

    BlockCapsule tampered = new BlockCapsule(local.getInstance());
    // tampered has no merkleValidated flag set
    try {
      tampered.validateMerkleRoot();
      Assert.fail("Expected BadBlockException for merkle mismatch");
    } catch (BadBlockException e) {
      Assert.assertTrue(e.getMessage().contains("merkle"));
    }
  }

  /* @Test
  public void testAddTransaction() {
    TransactionCapsule transactionCapsule = new TransactionCapsule("123", 1L);
    blockCapsule0.addTransaction(transactionCapsule);
    Assert.assertArrayEquals(blockCapsule0.getTransactions().get(0).getHash().getBytes(),
        transactionCapsule.getHash().getBytes());
    Assert.assertEquals(transactionCapsule.getInstance().getRawData().getVout(0).getValue(),
        blockCapsule0.getTransactions().get(0).getInstance().getRawData().getVout(0).getValue());
  } */

  @Test
  public void testGetData() {
    blockCapsule0.getData();
    byte[] b = blockCapsule0.getData();
    BlockCapsule blockCapsule1 = null;
    try {
      blockCapsule1 = new BlockCapsule(b);
      Assert.assertEquals(blockCapsule0.getBlockId(), blockCapsule1.getBlockId());
    } catch (BadItemException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testValidate() {

  }

  @Test
  public void testGetInsHash() {
    Assert.assertEquals(1,
        blockCapsule0.getInstance().getBlockHeader().getRawData().getNumber());
    Assert.assertEquals(blockCapsule0.getParentHash(),
        Sha256Hash.wrap(blockCapsule0.getParentHashStr()));
  }


  @Test
  public void testHasWitnessSignature() {

    localWitnesses = new LocalWitnesses();
    localWitnesses.setPrivateKeys(Arrays.asList(privateKey));
    localWitnesses.initWitnessAccountAddress(null, true);
    Args.setLocalWitnesses(localWitnesses);

    Assert.assertFalse(blockCapsule0.hasWitnessSignature());
    blockCapsule0
        .sign(ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));
    Assert.assertTrue(blockCapsule0.hasWitnessSignature());
  }

  @Test
  public void testGetTimeStamp() {
    Assert.assertEquals(1234L, blockCapsule0.getTimeStamp());
  }

  /**
   * Pin the contract that switchFork's signature recheck relies on:
   * when the recovered signer address does not match the witness address,
   * validateSignature returns false (no exception). switchFork uses the
   * boolean return to decide whether to throw, so this contract is what
   * makes the fix work for "wrong signer" attacks.
   */
  @Test
  public void testValidateSignatureReturnsFalseWhenSignerMismatch() throws Exception {
    String signerKey = PublicMethod.getRandomPrivateKey();
    String witnessKey = PublicMethod.getRandomPrivateKey();
    byte[] witnessAddress = PublicMethod.getAddressByteByPrivateKey(witnessKey);

    BlockCapsule block = new BlockCapsule(2,
        Sha256Hash.wrap(ByteString.copyFrom(ByteArray.fromHexString(
            "9938a342238077182498b464ac0292229938a342238077182498b464ac029222"))),
        4321,
        ByteString.copyFrom(witnessAddress));
    block.sign(ByteArray.fromHexString(signerKey));

    DynamicPropertiesStore dps = mock(DynamicPropertiesStore.class);
    when(dps.getAllowMultiSign()).thenReturn(0L);
    AccountStore accountStore = mock(AccountStore.class);

    Assert.assertFalse(block.validateSignature(dps, accountStore));
  }

  /**
   * Same key path under the happy case: when signer == witness, validateSignature
   * returns true. Guards against any future refactor that accidentally inverts
   * the comparison or strips the witness check.
   */
  @Test
  public void testValidateSignatureReturnsTrueWhenSignerMatches() throws Exception {
    String key = PublicMethod.getRandomPrivateKey();
    byte[] witnessAddress = PublicMethod.getAddressByteByPrivateKey(key);

    BlockCapsule block = new BlockCapsule(3,
        Sha256Hash.wrap(ByteString.copyFrom(ByteArray.fromHexString(
            "9938a342238077182498b464ac0292229938a342238077182498b464ac029222"))),
        5678,
        ByteString.copyFrom(witnessAddress));
    block.sign(ByteArray.fromHexString(key));

    DynamicPropertiesStore dps = mock(DynamicPropertiesStore.class);
    when(dps.getAllowMultiSign()).thenReturn(0L);
    AccountStore accountStore = mock(AccountStore.class);

    Assert.assertTrue(block.validateSignature(dps, accountStore));
  }

  /**
   * The other failure mode switchFork must handle: signature bytes are
   * malformed (cannot recover a public key). validateSignature wraps the
   * underlying SignatureException as ValidateSignatureException, which the
   * existing catch block in switchFork already handles.
   */
  @Test(expected = ValidateSignatureException.class)
  public void testValidateSignatureThrowsForMalformedSignature() throws Exception {
    byte[] witnessAddress = PublicMethod.getAddressByteByPrivateKey(
        PublicMethod.getRandomPrivateKey());

    // 65-byte signature with valid length but garbage content — passes Rsv parsing
    // but fails ECDSA recovery, surfacing SignatureException → ValidateSignatureException.
    byte[] garbageSigBytes = new byte[65];
    Arrays.fill(garbageSigBytes, (byte) 0xAB);
    ByteString garbageSig = ByteString.copyFrom(garbageSigBytes);

    BlockHeader.raw rawData = BlockHeader.raw.newBuilder()
        .setNumber(4)
        .setTimestamp(1111)
        .setParentHash(ByteString.copyFrom(ByteArray.fromHexString(
            "9938a342238077182498b464ac0292229938a342238077182498b464ac029222")))
        .setWitnessAddress(ByteString.copyFrom(witnessAddress))
        .build();
    BlockHeader header = BlockHeader.newBuilder()
        .setRawData(rawData)
        .setWitnessSignature(garbageSig)
        .build();
    Block proto = Block.newBuilder().setBlockHeader(header).build();
    BlockCapsule block = new BlockCapsule(proto);

    DynamicPropertiesStore dps = mock(DynamicPropertiesStore.class);
    when(dps.getAllowMultiSign()).thenReturn(0L);
    AccountStore accountStore = mock(AccountStore.class);

    block.validateSignature(dps, accountStore);
  }

  @Test
  public void testConcurrentToString() throws InterruptedException {
    List<Thread> threadList = new ArrayList<>();
    int n = 10;
    for (int i = 0; i < n; i++) {
      threadList.add(new Thread(() -> blockCapsule0.toString()));
    }
    for (int i = 0; i < n; i++) {
      threadList.get(i).start();
    }
    for (int i = 0; i < n; i++) {
      threadList.get(i).join();
    }
    Assert.assertTrue(true);
  }

}
