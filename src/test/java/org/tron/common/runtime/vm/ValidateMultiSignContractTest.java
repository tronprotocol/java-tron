package org.tron.common.runtime.vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.PrecompiledContracts.ValidateMultiSign;
import org.tron.common.storage.Deposit;
import org.tron.common.storage.DepositImpl;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class ValidateMultiSignContractTest {

  private static TronApplicationContext context;
  private static Application appT;
  private static Manager dbManager;

  private static final String dbPath = "output_PrecompiledContracts_test";

  private static final String METHOD_SIGN = "validatemultisign(address,uint256,bytes32,bytes[])";
  ValidateMultiSign contract = new ValidateMultiSign();

  private static final byte[] longData;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    dbManager = context.getBean(Manager.class);
    longData = new byte[100000000];
    Arrays.fill(longData, (byte) 2);
  }


  @Test
  void correctionTest() {
    byte[] hash = Hash.sha3(longData);
    ECKey key = new ECKey();
    byte[] sign = key.sign(hash).toByteArray();
    List<Object> signs = new ArrayList<>();
    signs.add(Hex.toHexString(sign));

    Assert.assertEquals(
        validateMultiSign(Wallet.encode58Check(key.getAddress()), 1, hash, signs)
            .getValue()
        , DataWord.ZERO().getData());
  }

  Pair<Boolean, byte[]> validateMultiSign(String address, int permissionId, byte[] hash,
      List<Object> signatures) {
    List<Object> parameters = Arrays
        .asList(address, permissionId, "0x" + Hex.toHexString(hash), signatures);
    byte[] input = Hex.decode(AbiUtil.parseParameters(METHOD_SIGN, parameters));
    Deposit deposit = DepositImpl.createRoot(dbManager);
    logger.info("energy for data:{}", contract.getEnergyForData(input));
    contract.setDeposit(deposit);

    Pair<Boolean, byte[]> ret = contract.execute(input);

    logger.info("BytesArray:{}，HexString:{}", Arrays.toString(ret.getValue()),
        Hex.toHexString(ret.getValue()));
    return ret;
  }


}
