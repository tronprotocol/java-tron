package org.tron.common.runtime.vm;

import static org.tron.common.utils.ByteUtil.hexToBytes;
import static org.tron.common.utils.ByteUtil.longTo32Bytes;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.WalletUtil;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class AllowTvmCompatibleEvmTest extends VMTestBase {
  /*contract c {
    function getRipemd160() public view returns(bytes32 output) {
      string memory input = "11";
      assembly {
        if iszero(staticcall(not(0), 0x20003, add(input, 0x20), 0x2, output, 0x20)) {
          revert(0, 0)
        }
        output := mload(add(output,0x0c))
      }

    }
  }*/

  @BeforeClass
  public static void beforeClass() {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmIstanbul(1);
    VMConfig.initAllowTvmLondon(1);
    VMConfig.initAllowTvmCompatibleEvm(1);
  }

  @Test
  public void testEthRipemd160() throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    String contractName = "testEthRipemd160";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String abi = "[{\"inputs\":[],\"name\":\"getRipemd160\","
        + "\"outputs\":[{\"internalType\":\"bytes32\",\"name\":\"output\",\"type\":\"bytes32\"}],"
        + "\"stateMutability\":\"view\",\"type\":\"function\"}]";
    String factoryCode = "608060405234801561001057600080fd5b5060bc"
        + "8061001f6000396000f3fe6080604052348015600f57600080fd5b506"
        + "004361060285760003560e01c8063c99ea27e14602d575b600080fd5b"
        + "60336045565b60405190815260200160405180910390f35b600080604"
        + "05180604001604052806002815260200161313160f01b815250905060"
        + "208260026020840162020003600019fa607e57600080fd5b50600c015"
        + "19056fea2646970667358221220d2f0f2cba312fd79e42db9191048a6"
        + "1e0eea399c580d9f2b3c13dd154afebfe164736f6c63430008070033";
    long value = 0;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, abi, factoryCode, value, feeLimit, consumeUserResourcePercent,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootRepository, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: getRipemd160()
    String methodByAddr = "getRipemd160()";
    String hexInput =
        AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));

    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertEquals(returnValue,
        hexToBytes("a76d892cc3522eab763529dfc84b12c080ee1"
            + "fe8000000000000000000000000"));
  }

  /*contract c {
    function F(uint32 rounds, bytes32[2] memory h, bytes32[4] memory m,
     bytes8[2] memory t, bool f) public view returns (bytes32[2] memory) {
      bytes32[2] memory output;

      bytes memory args = abi.encodePacked(
        rounds, h[0], h[1], m[0], m[1], m[2], m[3], t[0], t[1], f);

      assembly {
        if iszero(staticcall(not(0), 0x20009, add(args, 32), 0xd5, output, 0x40)) {
          revert(0, 0)
        }
      }

      return output;
    }

    function callF() public view returns (bytes32[2] memory) {
      uint32 rounds = 12;

      bytes32[2] memory h;
      h[0] = hex"48c9bdf267e6096a3ba7ca8485ae67bb2bf894fe72f36e3cf1361d5f3af54fa5";
      h[1] = hex"d182e6ad7f520e511f6c3e2b8c68059b6bbd41fbabd9831f79217e1319cde05b";

      bytes32[4] memory m;
      m[0] = hex"6162630000000000000000000000000000000000000000000000000000000000";
      m[1] = hex"0000000000000000000000000000000000000000000000000000000000000000";
      m[2] = hex"0000000000000000000000000000000000000000000000000000000000000000";
      m[3] = hex"0000000000000000000000000000000000000000000000000000000000000000";

      bytes8[2] memory t;
      t[0] = hex"03000000";
      t[1] = hex"00000000";

      bool f = true;

      // Expected output:
      // ba80a53f981c4d0d6a2797b69f12f6e94c212f14685ac4b74b12bb6fdbffa2d1
      // 7d87c5392aab792dc252d5de4533cc9518d38aa8dbf1925ab92386edd4009923
      return F(rounds, h, m, t, f);
    }
  }*/

  @Test
  public void testBlake2f() throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    String contractName = "testBlake2f";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String abi = "[{\"inputs\":[{\"internalType\":\"uint32\",\"name\":\"rounds\","
        + "\"type\":\"uint32\"},{\"internalType\":\"bytes32[2]\",\"name\":\"h\","
        + "\"type\":\"bytes32[2]\"},{\"internalType\":\"bytes32[4]\",\"name\":\"m\","
        + "\"type\":\"bytes32[4]\"},{\"internalType\":\"bytes8[2]\",\"name\":\"t\","
        + "\"type\":\"bytes8[2]\"},{\"internalType\":\"bool\",\"name\":\"f\",\"type\":\"bool\"}],"
        + "\"name\":\"F\",\"outputs\":[{\"internalType\":\"bytes32[2]\",\"name\":\"\","
        + "\"type\":\"bytes32[2]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},"
        + "{\"inputs\":[],\"name\":\"callF\",\"outputs\":[{\"internalType\":\"bytes32[2]\","
        + "\"name\":\"\",\"type\":\"bytes32[2]\"}],\"stateMutability\":\"view\","
        + "\"type\":\"function\"}]";
    String factoryCode = "608060405234801561001057600080fd5b506104b28061"
        + "00206000396000f3fe608060405234801561001057600080fd5b50600436106"
        + "100365760003560e01c806372de3cbd1461003b578063fc75ac471461006457"
        + "5b600080fd5b61004e61004936600461035a565b61006c565b60405161005b9"
        + "190610414565b60405180910390f35b61004e610165565b610074610215565b"
        + "61007c610215565b60008787826020020151886001602002015188600060200"
        + "2015189600160200201518a600260200201518b600360200201518b60006020"
        + "0201518c6001602090810291909101516040516001600160e01b031960e09b9"
        + "09b1b9a909a16918a0191909152602489019790975260448801959095526064"
        + "870193909352608486019190915260a485015260c48401526001600160c01b0"
        + "31990811660e48401521660ec82015284151560f81b60f482015260f5016040"
        + "51602081830303815290604052905060408260d56020840162020009600019f"
        + "a61015a57600080fd5b509695505050505050565b61016d610215565b600c61"
        + "0177610215565b7f48c9bdf267e6096a3ba7ca8485ae67bb2bf894fe72f36e3"
        + "cf1361d5f3af54fa581527fd182e6ad7f520e511f6c3e2b8c68059b6bbd41fb"
        + "abd9831f79217e1319cde05b60208201526101c8610233565b6261626360e81"
        + "b81526000602082018190526040820181905260608201526101ee610215565b"
        + "600360f81b815260006020820152600161020b858585858561006c565b95505"
        + "05050505090565b604051806040016040528060029060208202803683375091"
        + "92915050565b604051806080016040528060049060208202803683375091929"
        + "15050565b600082601f83011261026257600080fd5b60405160808101818110"
        + "67ffffffffffffffff8211171561029357634e487b7160e01b6000526041600"
        + "45260246000fd5b6040528083608081018610156102a857600080fd5b60005b"
        + "60048110156102ca5781358352602092830192909101906001016102ab565b5"
        + "09195945050505050565b600082601f8301126102e657600080fd5b6102ee61"
        + "0445565b80838560408601111561030057600080fd5b6000805b60028110156"
        + "103395782356001600160c01b031981168114610324578283fd5b8552602094"
        + "8501949290920191600101610304565b50919695505050505050565b8035801"
        + "515811461035557600080fd5b919050565b6000806000806000610140868803"
        + "121561037357600080fd5b853563ffffffff8116811461038757600080fd5b9"
        + "4506020603f8701881361039a57600080fd5b6103a2610445565b8082890160"
        + "608a018b8111156103b757600080fd5b60005b60028110156103d7578235855"
        + "293850193918501916001016103ba565b508298506103e58c82610251565b97"
        + "5050505050506103f98760e088016102d5565b9150610408610120870161034"
        + "5565b90509295509295909350565b60408101818360005b600281101561043c"
        + "57815183526020928301929091019060010161041d565b50505092915050565"
        + "b6040805190810167ffffffffffffffff8111828210171561047657634e487b"
        + "7160e01b600052604160045260246000fd5b6040529056fea26469706673582"
        + "21220a4795cc7f3618b8a552add0f65ee25825ffc8b8077ec1f838fe8dbae5c"
        + "3f412364736f6c63430008070033";
    long value = 0;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, abi, factoryCode, value, feeLimit, consumeUserResourcePercent,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootRepository, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: callF()
    String methodByAddr = "callF()";
    String hexInput =
        AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));

    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertEquals(returnValue,
        hexToBytes("ba80a53f981c4d0d6a2797b69f12f6e94c212f14685ac4b74b12bb6fdbffa2d17d87"
            + "c5392aab792dc252d5de4533cc9518d38aa8dbf1925ab92386edd4009923"));
  }

  /*contract c {
    function getprice() public view returns(uint) {
    return tx.gasprice;
    }
  }*/

  @Test
  public void testGasPrice() throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    String contractName = "testGasPrice";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String abi = "[{\"inputs\":[],\"name\":\"getprice\","
        + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],"
        + "\"stateMutability\":\"view\",\"type\":\"function\"}]";
    String factoryCode = "6080604052348015600f57600080fd5b50607680601d"
        + "6000396000f3fe6080604052348015600f57600080fd5b506004361060285"
        + "760003560e01c80630fcb598414602d575b600080fd5b3a60405190815260"
        + "200160405180910390f3fea2646970667358221220ee994af43fb1d2a4594"
        + "ae1355099296fc098d01a7dbe9056530031db6fb9b9c464736f6c63430008070033";
    long value = 0;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, abi, factoryCode, value, feeLimit, consumeUserResourcePercent,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootRepository, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: getprice()
    String methodByAddr = "getprice()";
    String hexInput =
        AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));

    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertEquals(returnValue,
        longTo32Bytes(manager.getDynamicPropertiesStore().getEnergyFee()));
  }

  @Test
  public void testChainId() throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    String contractName = "TestChainId";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String contractCode = "608060405234801561001057600080fd5b5060b58061001f6000"
        + "396000f3fe6080604052348015600f57600080fd5b506004361060285760003560e01c"
        + "80633408e47014602d575b600080fd5b60336047565b604051603e9190605c565b6040"
        + "5180910390f35b600046905090565b6056816075565b82525050565b60006020820190"
        + "50606f6000830184604f565b92915050565b600081905091905056fea2646970667358"
        + "2212203ccbe28f012f703b4369308e34d6dfc1a89a5f51e3ea42d531fcf3a2dba31150"
        + "64736f6c63430008070033";
    long feeLimit = 100_000_000L;

    // deploy contract
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, "[]", contractCode, 0, feeLimit, 0, null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootRepository, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: getChainId()
    String methodSignature = "getChainId()";
    String hexInput =
        AbiUtil.parseMethod(methodSignature, Collections.singletonList(""));

    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000028c12d1e");

    VMConfig.initAllowTvmCompatibleEvm(0);
  }

  @AfterClass
  public static void afterClass() {
    ConfigLoader.disable = false;
    VMConfig.initAllowTvmTransferTrc10(0);
    VMConfig.initAllowTvmConstantinople(0);
    VMConfig.initAllowTvmSolidity059(0);
    VMConfig.initAllowTvmIstanbul(0);
    VMConfig.initAllowTvmCompatibleEvm(0);
  }

}
