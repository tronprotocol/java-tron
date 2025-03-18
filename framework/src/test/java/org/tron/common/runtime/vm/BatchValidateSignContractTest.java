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
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.client.utils.AbiUtil;
import org.tron.core.db.TransactionTrace;
import org.tron.core.vm.PrecompiledContracts;
import org.tron.core.vm.PrecompiledContracts.BatchValidateSign;


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
