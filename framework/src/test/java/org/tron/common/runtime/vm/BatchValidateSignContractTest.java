package org.tron.common.runtime.vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.client.utils.AbiUtil;
import org.tron.core.db.TransactionTrace;
import org.tron.core.vm.PrecompiledContracts;
import org.tron.core.vm.PrecompiledContracts.BatchValidateSign;
import org.tron.core.vm.config.VMConfig;


@Slf4j
public class BatchValidateSignContractTest {

  private static final String METHOD_SIGN = "batchvalidatesign(bytes32,bytes[],address[])";
  private static final byte[] smellData;
  private static final byte[] longData;

  static {
    smellData = new byte[10];
    longData = new byte[1000];
    Arrays.fill(smellData, (byte) 1);
    Arrays.fill(longData, (byte) 2);
  }

  PrecompiledContracts.BatchValidateSign contract = new BatchValidateSign();

  @Test
  public void staticCallTest() {
    contract.setConstantCall(true);
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(longData);
    //insert incorrect
    for (int i = 0; i < 16; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      if (i % 5 == 0) {
        signatures.add(Hex.toHexString(DataWord.ONE().getData()));
      } else {
        signatures.add(Hex.toHexString(sign));
      }
      if (i == 13) {
        addresses
            .add(StringUtil.encode58Check(TransactionTrace.convertToTronAddress(new byte[20])));
      } else {
        addresses.add(StringUtil.encode58Check(key.getAddress()));
      }
    }
    Pair<Boolean, byte[]> ret;
    ret = validateMultiSign(hash, signatures, addresses);
    for (int i = 0; i < 16; i++) {
      if (i % 5 == 0) {
        Assert.assertEquals(ret.getValue()[i], 0);
      } else if (i == 13) {
        Assert.assertEquals(ret.getValue()[i], 0);
      } else {
        Assert.assertEquals(ret.getValue()[i], 1);
      }
    }

    //test when length >= 16
    signatures.add(Hex.toHexString(DataWord.ONE().getData()));
    addresses
        .add(StringUtil.encode58Check(TransactionTrace.convertToTronAddress(new byte[20])));
    ret = validateMultiSign(hash, signatures, addresses);
    Assert.assertEquals(ret.getValue().length, 32);
    Assert.assertArrayEquals(ret.getValue(), new byte[32]);

    //after optimized
    VMConfig.initAllowTvmSelfdestructRestriction(1);
    ret = validateMultiSign(hash, signatures, addresses);
    Assert.assertEquals(ret.getValue().length, 32);
    Assert.assertArrayEquals(ret.getValue(), new byte[32]);
    VMConfig.initAllowTvmSelfdestructRestriction(0);
    System.gc(); // force triggering full gc to avoid timeout for next test
  }

  @Test
  public void correctionTest() {
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(longData);
    //insert incorrect every 5 pairs
    for (int i = 0; i < 16; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      if (i % 5 == 0) {
        addresses.add(StringUtil.encode58Check(TransactionTrace
            .convertToTronAddress(new byte[20])));
        signatures.add(Hex.toHexString(DataWord.ONE().getData()));
      } else {
        addresses.add(StringUtil.encode58Check(key.getAddress()));
        signatures.add(Hex.toHexString(sign));
      }
    }
    Pair<Boolean, byte[]> ret = null;
    ret = validateMultiSign(hash, signatures, addresses);
    for (int i = 0; i < 32; i++) {
      if (i >= 16) {
        Assert.assertEquals(ret.getValue()[i], 0);
      } else if (i % 5 == 0) {
        Assert.assertEquals(ret.getValue()[i], 0);
      } else {
        Assert.assertEquals(ret.getValue()[i], 1);
      }
    }

    // incorrect hash
    byte[] incorrectHash = DataWord.ONE().getData();
    ret = validateMultiSign(incorrectHash, signatures, addresses);
    for (int i = 0; i < 16; i++) {
      Assert.assertEquals(ret.getValue()[i], 0);
    }
    // different length
    byte[] incorrectSign = DataWord.ONE().getData();
    List<Object> incorrectSigns = new ArrayList<>(signatures);
    incorrectSigns.remove(incorrectSigns.size() - 1);
    ret = validateMultiSign(hash, incorrectSigns, addresses);
    Assert.assertArrayEquals(ret.getValue(), DataWord.ZERO().getData());
    System.gc(); // force triggering full gc to avoid timeout for next test
  }

  // TIP-854: after activation, batchValidateSign (H=5, I=6) must reject calldata
  // whose byte length is incompatible with the (words - 5) / 6 shape the per-call
  // energy formula already assumes, returning (false, empty). The guard lives in
  // doExecute(); the outer try/catch does not mask it because the guard does not
  // throw (pure arithmetic + a static getter).
  @Test
  public void testTip854RejectsMalformedCalldata() {
    contract.setVmShouldEndInUs(System.nanoTime() / 1000 + 2_000_000);
    VMConfig.initAllowTvmOsaka(1);
    try {
      // Bucket 1: 32-aligned head + sub-word trailing bytes (r=1, r=31).
      for (int r : new int[]{1, 31}) {
        byte[] data = new byte[(5 + 6) * 32 + r];
        Pair<Boolean, byte[]> ret = contract.execute(data);
        Assert.assertFalse("non-32-aligned len=" + data.length, ret.getLeft());
        Assert.assertSame(ByteUtil.EMPTY_BYTE_ARRAY, ret.getRight());
      }
      // Bucket 2: fewer than the static head's 5 words.
      for (int bytes : new int[]{0, 32, 64, 96, 128}) {
        Pair<Boolean, byte[]> ret = contract.execute(new byte[bytes]);
        Assert.assertFalse("len=" + bytes + " < 5 words", ret.getLeft());
        Assert.assertSame(ByteUtil.EMPTY_BYTE_ARRAY, ret.getRight());
      }
      // Bucket 3: 32-aligned but tail not a multiple of I=6 words (k = 1..5).
      for (int k = 1; k <= 5; k++) {
        byte[] data = new byte[(5 + k) * 32];
        Pair<Boolean, byte[]> ret = contract.execute(data);
        Assert.assertFalse("aligned bad-tail k=" + k, ret.getLeft());
        Assert.assertSame(ByteUtil.EMPTY_BYTE_ARRAY, ret.getRight());
      }
      // Null calldata: explicit spec clause.
      Pair<Boolean, byte[]> ret = contract.execute(null);
      Assert.assertFalse("null calldata", ret.getLeft());
      Assert.assertSame(ByteUtil.EMPTY_BYTE_ARRAY, ret.getRight());
    } finally {
      VMConfig.initAllowTvmOsaka(0);
    }
    System.gc();
  }

  // TIP-854 Compatibility: for canonically-shaped calldata — all 65-byte real
  // signatures so each bytes[i] encodes in exactly 4 words (1 length + 3 content)
  // — total length equals 5*32 + 6*32*N, so pre- and post-activation must be
  // observationally identical.
  @Test
  public void testTip854CanonicalInputUnchanged() {
    contract.setConstantCall(true);
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(longData);
    for (int i = 0; i < 8; i++) {
      ECKey key = new ECKey();
      signatures.add(Hex.toHexString(key.sign(hash).toByteArray()));
      addresses.add(StringUtil.encode58Check(key.getAddress()));
    }

    VMConfig.initAllowTvmOsaka(0);
    Pair<Boolean, byte[]> pre = validateMultiSign(hash, signatures, addresses);
    VMConfig.initAllowTvmOsaka(1);
    try {
      Pair<Boolean, byte[]> post = validateMultiSign(hash, signatures, addresses);
      Assert.assertEquals(pre.getLeft(), post.getLeft());
      Assert.assertArrayEquals(pre.getValue(), post.getValue());
    } finally {
      VMConfig.initAllowTvmOsaka(0);
    }
    System.gc();
  }

  // TIP-854: before activation the guard is not consulted. Malformed calldata
  // that would raise inside doExecute gets collapsed to (true, 32-byte zero) by
  // the outer catch — this is the legacy behaviour and must be preserved.
  @Test
  public void testTip854PreActivationNoOp() {
    VMConfig.initAllowTvmOsaka(0);
    contract.setVmShouldEndInUs(System.nanoTime() / 1000 + 2_000_000);
    Pair<Boolean, byte[]> ret = contract.execute(new byte[(5 + 1) * 32]);
    Assert.assertTrue("pre-activation must not take the new reject path", ret.getLeft());
    Assert.assertEquals(32, ret.getRight().length);
  }

  Pair<Boolean, byte[]> validateMultiSign(byte[] hash, List<Object> signatures,
      List<Object> addresses) {
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    byte[] input = Hex.decode(AbiUtil.parseParameters(METHOD_SIGN, parameters));
    contract.getEnergyForData(input);
    long maxExecutionTime = 2000; // ms
    contract.setVmShouldEndInUs(System.nanoTime() / 1000 + maxExecutionTime * 1000);
    Pair<Boolean, byte[]> ret = contract.execute(input);
    logger.info("BytesArray:{}，HexString:{}", Arrays.toString(ret.getValue()),
        Hex.toHexString(ret.getValue()));
    return ret;
  }


}
