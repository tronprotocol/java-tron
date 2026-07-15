package org.tron.common.runtime.vm;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.WalletUtil;
import org.tron.common.utils.client.utils.AbiUtil;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.protos.Protocol.Transaction;

public class TvmIssueVerifierTest extends VMTestBase {

  private static final int WORD_SIZE = 32;

  private static final String ABI =
      "[{\"inputs\":[{\"internalType\":\"bytes\",\"name\":\"code\",\"type\":\"bytes\"},"
          + "{\"internalType\":\"uint256\",\"name\":\"salt\",\"type\":\"uint256\"}],"
          + "\"name\":\"failedCreate2KeepsPriorReturnData\","
          + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"beforeSize\","
          + "\"type\":\"uint256\"},{\"internalType\":\"address\",\"name\":\"created\","
          + "\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"afterSize\","
          + "\"type\":\"uint256\"}],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
          + "{\"inputs\":[],\"name\":\"modexpZeroModulus\","
          + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"ok\","
          + "\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"sizeAfter\","
          + "\"type\":\"uint256\"},{\"internalType\":\"bytes32\",\"name\":\"copiedWord\","
          + "\"type\":\"bytes32\"}],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
          + "{\"inputs\":[{\"internalType\":\"bytes\",\"name\":\"code\",\"type\":\"bytes\"},"
          + "{\"internalType\":\"uint256\",\"name\":\"salt\",\"type\":\"uint256\"}],"
          + "\"name\":\"successfulCreate2KeepsPriorReturnData\","
          + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"beforeSize\","
          + "\"type\":\"uint256\"},{\"internalType\":\"address\",\"name\":\"created\","
          + "\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"afterSize\","
          + "\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"createdSize\","
          + "\"type\":\"uint256\"}],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

  private static final String BYTECODE =
      "6080604052348015600f57600080fd5b506105198061001f6000396000f3fe6080604052348015610010"
          + "57600080fd5b50600436106100415760003560e01c80634ecba0f014610046578063543b525514610079"
          + "5780639fefb5fd146100ab575b600080fd5b610060600480360381019061005b919061036b565b6100cb"
          + "565b6040516100709493929190610417565b60405180910390f35b610093600480360381019061008e91"
          + "9061036b565b610104565b6040516100a29392919061045c565b60405180910390f35b6100b361013656"
          + "5b6040516100c2939291906104ac565b60405180910390f35b6000806000806112346000526020600060"
          + "2060008060045af1503d9350848651602088016000f592503d9150823b905092959194509250565b6000"
          + "80600061123460005260206000602060008060045af1503d9250838551602087016001f591503d905092"
          + "50925092565b600080600080606367ffffffffffffffff8111156101575761015661020a565b5b604051"
          + "9080825280601f01601f1916602001820160405280156101895781602001600182028036833780820191"
          + "505090505b50905060208101600181526001602082015260016040820152600260608201536003606182"
          + "01536000606282015360001960005260206000606383600060055af194503d9350600051925050509091"
          + "92565b6000604051905090565b600080fd5b600080fd5b600080fd5b600080fd5b6000601f19601f8301"
          + "169050919050565b7f4e487b710000000000000000000000000000000000000000000000000000000060"
          + "0052604160045260246000fd5b610242826101f9565b810181811067ffffffffffffffff821117156102"
          + "615761026061020a565b5b80604052505050565b60006102746101db565b90506102808282610239565b"
          + "919050565b600067ffffffffffffffff8211156102a05761029f61020a565b5b6102a9826101f9565b90"
          + "50602081019050919050565b82818337600083830152505050565b60006102d86102d384610285565b61"
          + "026a565b9050828152602081018484840111156102f4576102f36101f4565b5b6102ff8482856102b656"
          + "5b509392505050565b600082601f83011261031c5761031b6101ef565b5b813561032c84826020860161"
          + "02c5565b91505092915050565b6000819050919050565b61034881610335565b811461035357600080fd"
          + "5b50565b6000813590506103658161033f565b92915050565b6000806040838503121561038257610381"
          + "6101e5565b5b600083013567ffffffffffffffff8111156103a05761039f6101ea565b5b6103ac858286"
          + "01610307565b92505060206103bd85828601610356565b9150509250929050565b6103d081610335565b"
          + "82525050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b60006104"
          + "01826103d6565b9050919050565b610411816103f6565b82525050565b600060808201905061042c6000"
          + "8301876103c7565b6104396020830186610408565b61044660408301856103c7565b6104536060830184"
          + "6103c7565b95945050505050565b600060608201905061047160008301866103c7565b61047e60208301"
          + "85610408565b61048b60408301846103c7565b949350505050565b6000819050919050565b6104a68161"
          + "0493565b82525050565b60006060820190506104c160008301866103c7565b6104ce60208301856103c7"
          + "565b6104db604083018461049d565b94935050505056fea2646970667358221220c9b28608a5295f3b52"
          + "702e75aa5d40b18593bd0a9ff2e03e2274edbd42642c6a64736f6c634300081e0033";

  @Before
  public void enableVmFeatures() {
    ConfigLoader.disable = false;
    manager.getDynamicPropertiesStore().saveAllowTvmTransferTrc10(1);
    manager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    manager.getDynamicPropertiesStore().saveAllowTvmIstanbul(1);
    manager.getDynamicPropertiesStore().saveAllowTvmLondon(1);
    manager.getDynamicPropertiesStore().saveAllowTvmCompatibleEvm(1);
  }

  @Test
  public void verifyTvmOsakaFixesWithSolidity()
      throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    byte[] verifierAddress = deployVerifier();

    manager.getDynamicPropertiesStore().saveAllowTvmOsaka(0);

    TVMTestResult modExpResult =
        trigger(verifierAddress, "modexpZeroModulus()", Collections.emptyList(), 100_000_000L);
    byte[] modExpReturn = modExpResult.getRuntime().getResult().getHReturn();
    Assert.assertNull(modExpResult.getRuntime().getRuntimeError());
    Assert.assertEquals(BigInteger.ONE, word(modExpReturn, 0));
    Assert.assertEquals("MODEXP zero modulus currently returns empty returndata",
        BigInteger.ZERO, word(modExpReturn, 1));

    TVMTestResult create2Result =
        trigger(verifierAddress, "failedCreate2KeepsPriorReturnData(bytes,uint256)",
            Arrays.asList("00", 7L), 100_000_000L);
    byte[] create2Return = create2Result.getRuntime().getResult().getHReturn();
    Assert.assertNull(create2Result.getRuntime().getRuntimeError());
    Assert.assertEquals(BigInteger.valueOf(32), word(create2Return, 0));
    Assert.assertEquals(BigInteger.ZERO, word(create2Return, 1));
    Assert.assertEquals("failed CREATE2 keeps the previous 32-byte return data buffer",
        BigInteger.valueOf(32), word(create2Return, 2));

    TVMTestResult preOsakaCreate2SuccessResult =
        trigger(verifierAddress, "successfulCreate2KeepsPriorReturnData(bytes,uint256)",
            Arrays.asList(initCodeReturningRuntime("00"), 8L), 100_000_000L);
    byte[] preOsakaCreate2SuccessReturn =
        preOsakaCreate2SuccessResult.getRuntime().getResult().getHReturn();
    Assert.assertNull(preOsakaCreate2SuccessResult.getRuntime().getRuntimeError());
    Assert.assertEquals(BigInteger.valueOf(32), word(preOsakaCreate2SuccessReturn, 0));
    Assert.assertTrue(word(preOsakaCreate2SuccessReturn, 1).signum() != 0);
    Assert.assertEquals(BigInteger.valueOf(32), word(preOsakaCreate2SuccessReturn, 2));
    Assert.assertEquals(BigInteger.ONE, word(preOsakaCreate2SuccessReturn, 3));

    manager.getDynamicPropertiesStore().saveAllowTvmOsaka(1);

    modExpResult =
        trigger(verifierAddress, "modexpZeroModulus()", Collections.emptyList(), 100_000_000L);
    modExpReturn = modExpResult.getRuntime().getResult().getHReturn();
    Assert.assertNull(modExpResult.getRuntime().getRuntimeError());
    Assert.assertEquals(BigInteger.ONE, word(modExpReturn, 0));
    Assert.assertEquals("MODEXP zero modulus returns modLen bytes after Osaka",
        BigInteger.ONE, word(modExpReturn, 1));

    create2Result =
        trigger(verifierAddress, "failedCreate2KeepsPriorReturnData(bytes,uint256)",
            Arrays.asList("00", 7L), 100_000_000L);
    create2Return = create2Result.getRuntime().getResult().getHReturn();
    Assert.assertNull(create2Result.getRuntime().getRuntimeError());
    Assert.assertEquals(BigInteger.valueOf(32), word(create2Return, 0));
    Assert.assertEquals(BigInteger.ZERO, word(create2Return, 1));
    Assert.assertEquals("failed CREATE2 clears the previous return data buffer after Osaka",
        BigInteger.ZERO, word(create2Return, 2));

    TVMTestResult create2SuccessResult =
        trigger(verifierAddress, "successfulCreate2KeepsPriorReturnData(bytes,uint256)",
            Arrays.asList(initCodeReturningRuntime("00"), 9L), 100_000_000L);
    byte[] create2SuccessReturn = create2SuccessResult.getRuntime().getResult().getHReturn();
    Assert.assertNull(create2SuccessResult.getRuntime().getRuntimeError());
    Assert.assertEquals(BigInteger.valueOf(32), word(create2SuccessReturn, 0));
    Assert.assertTrue(word(create2SuccessReturn, 1).signum() != 0);
    Assert.assertEquals("successful CREATE2 clears the previous return data buffer after Osaka",
        BigInteger.ZERO, word(create2SuccessReturn, 2));
    Assert.assertEquals(BigInteger.ONE, word(create2SuccessReturn, 3));
  }

  private byte[] deployVerifier()
      throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    byte[] owner = Hex.decode(OWNER_ADDRESS);
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        "TvmIssueVerifier", owner, ABI, BYTECODE, 0, 1_000_000_000L, 0, null);
    byte[] contractAddress = WalletUtil.generateContractAddress(trx);
    Assert.assertNull(TvmTestUtils
        .processTransactionAndReturnRuntime(trx, rootRepository, null)
        .getRuntimeError());
    return contractAddress;
  }

  private TVMTestResult trigger(byte[] contractAddress, String method, List<Object> args,
      long feeLimit)
      throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    String input = AbiUtil.parseMethod(method, args);
    return TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
        contractAddress, Hex.decode(input), 0, feeLimit, manager, null);
  }

  private static BigInteger word(byte[] data, int index) {
    int start = index * WORD_SIZE;
    return new BigInteger(1, Arrays.copyOfRange(data, start, start + WORD_SIZE));
  }

  private static String initCodeReturningRuntime(String runtimeCode) {
    byte[] runtime = Hex.decode(runtimeCode);
    Assert.assertTrue(runtime.length <= 255);

    byte[] initCode = new byte[12 + runtime.length];
    initCode[0] = 0x60;
    initCode[1] = (byte) runtime.length;
    initCode[2] = 0x60;
    initCode[3] = 0x0c;
    initCode[4] = 0x60;
    initCode[5] = 0x00;
    initCode[6] = 0x39;
    initCode[7] = 0x60;
    initCode[8] = (byte) runtime.length;
    initCode[9] = 0x60;
    initCode[10] = 0x00;
    initCode[11] = (byte) 0xf3;
    System.arraycopy(runtime, 0, initCode, 12, runtime.length);

    return Hex.toHexString(initCode);
  }
}
