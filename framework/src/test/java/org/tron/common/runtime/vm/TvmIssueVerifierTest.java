package org.tron.common.runtime.vm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class TvmIssueVerifierTest extends VMTestBase {

  private static final int WORD_SIZE = 32;

  private static final String ABI = loadResource("solidity/TvmIssueVerifier.abi");

  private static final String BYTECODE = loadResource("solidity/TvmIssueVerifier.bin");

  @Before
  public void enableVmFeatures() {
    ConfigLoader.disable = false;
    manager.getDynamicPropertiesStore().saveAllowTvmTransferTrc10(1);
    manager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    manager.getDynamicPropertiesStore().saveAllowTvmIstanbul(1);
    manager.getDynamicPropertiesStore().saveAllowTvmLondon(1);
    manager.getDynamicPropertiesStore().saveAllowTvmCompatibleEvm(1);
    manager.getDynamicPropertiesStore().saveAllowTvmOsaka(1);
  }

  @Test
  public void verifyTvmOsakaFixesWithSolidity()
      throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    byte[] verifierAddress = deployVerifier();

    manager.getDynamicPropertiesStore().saveAllowTvmOsaka(0);

    TVMTestResult modExpResult = trigger(verifierAddress, "modexpZeroModulus()",
        Collections.emptyList(), 100_000_000L);
    byte[] modExpReturn = modExpResult.getRuntime().getResult().getHReturn();
    Assert.assertNull(modExpResult.getRuntime().getRuntimeError());
    Assert.assertEquals(BigInteger.ONE, word(modExpReturn, 0));
    Assert.assertEquals("MODEXP zero modulus currently returns empty returndata",
        BigInteger.ZERO, word(modExpReturn, 1));

    TVMTestResult create2Result = trigger(verifierAddress,
        "failedCreate2KeepsPriorReturnData(bytes,uint256)", Arrays.asList("00", 7L),
        100_000_000L);
    byte[] create2Return = create2Result.getRuntime().getResult().getHReturn();
    Assert.assertNull(create2Result.getRuntime().getRuntimeError());
    Assert.assertEquals(BigInteger.valueOf(32), word(create2Return, 0));
    Assert.assertEquals(BigInteger.ZERO, word(create2Return, 1));
    Assert.assertEquals("failed CREATE2 keeps the previous 32-byte return data buffer",
        BigInteger.valueOf(32), word(create2Return, 2));

    TVMTestResult preOsakaCreate2SuccessResult = trigger(verifierAddress,
        "successfulCreate2KeepsPriorReturnData(bytes,uint256)",
        Arrays.asList(initCodeReturningRuntime("00"), 8L), 100_000_000L);
    byte[] preOsakaCreate2SuccessReturn =
        preOsakaCreate2SuccessResult.getRuntime().getResult().getHReturn();
    Assert.assertNull(preOsakaCreate2SuccessResult.getRuntime().getRuntimeError());
    Assert.assertEquals(BigInteger.valueOf(32), word(preOsakaCreate2SuccessReturn, 0));
    Assert.assertTrue(word(preOsakaCreate2SuccessReturn, 1).signum() != 0);
    Assert.assertEquals(BigInteger.valueOf(32), word(preOsakaCreate2SuccessReturn, 2));
    Assert.assertEquals(BigInteger.ONE, word(preOsakaCreate2SuccessReturn, 3));

    manager.getDynamicPropertiesStore().saveAllowTvmOsaka(1);

    modExpResult = trigger(verifierAddress, "modexpZeroModulus()",
        Collections.emptyList(), 100_000_000L);
    modExpReturn = modExpResult.getRuntime().getResult().getHReturn();
    Assert.assertNull(modExpResult.getRuntime().getRuntimeError());
    Assert.assertEquals(BigInteger.ONE, word(modExpReturn, 0));
    Assert.assertEquals("MODEXP zero modulus returns modLen bytes after Osaka",
        BigInteger.ONE, word(modExpReturn, 1));

    create2Result = trigger(verifierAddress,
        "failedCreate2KeepsPriorReturnData(bytes,uint256)", Arrays.asList("00", 7L),
        100_000_000L);
    create2Return = create2Result.getRuntime().getResult().getHReturn();
    Assert.assertNull(create2Result.getRuntime().getRuntimeError());
    Assert.assertEquals(BigInteger.valueOf(32), word(create2Return, 0));
    Assert.assertEquals(BigInteger.ZERO, word(create2Return, 1));
    Assert.assertEquals("failed CREATE2 clears the previous return data buffer after Osaka",
        BigInteger.ZERO, word(create2Return, 2));

    TVMTestResult create2SuccessResult = trigger(verifierAddress,
        "successfulCreate2KeepsPriorReturnData(bytes,uint256)",
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
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootRepository, null);
    Assert.assertNull(runtime.getRuntimeError());
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

  private static String loadResource(String resource) {
    try (InputStream in = TvmIssueVerifierTest.class.getClassLoader()
        .getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("Missing test resource: " + resource);
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int read;
      while ((read = in.read(buffer)) >= 0) {
        out.write(buffer, 0, read);
      }
      return out.toString("UTF-8").trim();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read test resource: " + resource, e);
    }
  }
}
