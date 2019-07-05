package org.tron.common.runtime.vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.PrecompiledContracts.MultiValidateSign;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.utils.AbiUtil;

public class MultiValidateSignContractTest {

  private static final String METHODSIGN = "multivalidate(bytes32,bytes[],address[])";
  PrecompiledContracts.MultiValidateSign contract = new MultiValidateSign();
  private static final String[] privateKeys = {
      "f33101ea976d90491dcb9669be568db8bbc1ad23d90be4dede094976b67d550e",
      "4521c13f65cc9f5c1daa56923b8598d4015801ad28379675c64106f5f6afec30",
      "7c4977817417495f4ca0c35ab3d5a25e247355d68f89f593f3fea2ab62c8644f",
      "7d5a7396d6430edb7f66aa5736ef388f2bea862c9259de8ad8c2cfe080f6f5a0",
      "541a2d585fcea7e9b1803df4eb49af0eb09f1fa2ce06aa5b8ed60ac95655d66d",
      "a21a3074d4d84685efaffcd7c04e3eccb541ec4c85f61c41a099cd598ad39825",
      "763009595dd132aaf2d248999f2c6e7ba0acbbd9a9dfd88f7c2c158d97327645",
      "03caf867c46aaf86d56aa446db80cb49305126b77bfaccfe57ab17bdb4993ccc",
      "3a54ba30e3ee41b602eca8fb3a3ca1f99f49a3d3ab5d8d646a2ccdd3ffd9c21d",
      "e901ef62b241b6f1577fd6ea34ef8b1c4b3ddee1e3c051b9e63f5ff729ad47a1"
  };

  private static final byte[] smellData;
  private static final byte[] longData;

  static {
    smellData = new byte[10];
    longData = new byte[100000000];
    Arrays.fill(smellData, (byte) 1);
    Arrays.fill(longData, (byte) 2);
  }

  class Case {
    int cnt;
  }

  @Test
  void timeCostTest() {
    timecost(1);
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
      int index = i % privateKeys.length;
      ECKey ecKey = ECKey.fromPrivate(Hex.decode(privateKeys[index]));
      byte[] sign = ecKey.sign(hash).toByteArray();
      byte[] address = ecKey.getAddress();
      signatures.add("0x" + Hex.toHexString(sign));
      addresses.add(Wallet.encode58Check(address));
    }

    long start =  System.currentTimeMillis();
    Pair<Boolean, byte[]> ret = validateMultiSign(hash, signatures, addresses);
    long timeCosts = System.currentTimeMillis() - start;
//    System.out.println("cnt:" + cnt + " timeCost:" + timeCosts + "ms" + " :" + (timeCosts * 1.0 / cnt) + " ret:" + !new DataWord(ret.getValue()).isZero());

  }

  Pair<Boolean, byte[]> validateMultiSign(byte[] hash, List<Object> signatures, List<Object> addresses) {
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    byte[] input = Hex.decode(AbiUtil.parseParameters(METHODSIGN, parameters));
    return contract.execute(input);
  }

  @Test
  void correctionTest() {
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    for (int i = 0; i < 27; i++) {

    }


  }




  @Test
  void energyCostTest() {

  }

}
