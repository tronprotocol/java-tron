package org.tron.common.runtime.vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.PrecompiledContracts.MultiValidateSign;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class MultiValidateSignContractTest {

  private static final String METHOD_SIGN = "multivalidatesign(bytes32,bytes[],address[])";
  PrecompiledContracts.MultiValidateSign contract = new MultiValidateSign();

  private static final byte[] smellData;
  private static final byte[] longData;

  static {
    smellData = new byte[10];
    longData = new byte[100000000];
    Arrays.fill(smellData, (byte) 1);
    Arrays.fill(longData, (byte) 2);
  }

  @Test
  void correctionTest() {
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(longData);
    for (int i = 0; i < 27; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(Wallet.encode58Check(key.getAddress()));
    }
    Pair<Boolean, byte[]> ret;

    // correct case
    ret = validateMultiSign(hash, signatures, addresses);
    Assert.assertEquals(ret.getValue(), DataWord.ONE().getData());

    // incorrect hash
    byte[] incorrectHash = DataWord.ONE().getData();
    ret = validateMultiSign(incorrectHash, signatures, addresses);
    Assert.assertEquals(ret.getValue(), DataWord.ZERO().getData());

    // incorrect signature
    byte[] incorrectSign = DataWord.ONE().getData();
    List<Object> incorrectSigns = new ArrayList<>(signatures);
    incorrectSigns.remove(incorrectSigns.size() - 1);
    incorrectSigns.add(Hex.toHexString(incorrectSign));
    ret = validateMultiSign(incorrectHash, signatures, addresses);
    Assert.assertEquals(ret.getValue(), DataWord.ZERO().getData());

    // incorrect address
    byte[] address = DataWord.ONE().getData();
    List<Object> incorrectAddresses = new ArrayList<>(signatures);
    incorrectAddresses.remove(incorrectSigns.size() - 1);
    incorrectAddresses.add(Hex.toHexString(address));
    ret = validateMultiSign(incorrectHash, signatures, addresses);
    Assert.assertEquals(ret.getValue(), DataWord.ZERO().getData());
  }

  // just test timecosts
  //  @Test
  @Test(enabled = false)
  void timeCostTest() {
    // for warming up
    // timecost(1);

    int cnt = 27;
    for (;cnt <= 32; cnt++) {
      timecost(cnt);
    }
    int i = 1;
    while (i < 5) {
      cnt *= 2;
      i++;
      timecost(cnt);
    }
  }

  void timecost(int cnt) {

    byte[] data = smellData;
    byte[] hash = Hash.sha3(data);
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    for (int i = 0; i < cnt; i++) {
      ECKey ecKey = new ECKey();
      byte[] sign = ecKey.sign(hash).toByteArray();
      byte[] address = ecKey.getAddress();
      signatures.add("0x" + Hex.toHexString(sign));
      addresses.add(Wallet.encode58Check(address));
    }

    long start =  System.currentTimeMillis();
    Pair<Boolean, byte[]> ret = validateMultiSign(hash, signatures, addresses);
    Assert.assertEquals(ret.getValue(), DataWord.ONE().getData());
    long timeCosts = System.currentTimeMillis() - start;
    logger.info("cnt:" + cnt + " timeCost:" + timeCosts + "ms" + " :" + (timeCosts * 1.0 / cnt) + " ret:" + !new DataWord(ret.getValue()).isZero());

  }

  Pair<Boolean, byte[]> validateMultiSign(byte[] hash, List<Object> signatures, List<Object> addresses) {
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    byte[] input = Hex.decode(AbiUtil.parseParameters(METHOD_SIGN, parameters));
    contract.getEnergyForData(input);
    return contract.execute(input);
  }
}
