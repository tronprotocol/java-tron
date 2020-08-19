package org.tron.common.runtime.vm;

import static org.tron.common.utils.WalletUtil.generateContractAddress2;
import static org.tron.core.db.TransactionTrace.convertToTronAddress;

import java.util.Arrays;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.WalletUtil;
import org.tron.core.Wallet;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.vm.program.Program.OutOfEnergyException;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.DataWord;

public class Create2IstanbulTest extends VMTestBase {

  /*
pragma solidity ^0.5.12;

contract A {
  function deploy(bytes memory code, uint256 salt) public returns(address) {
       address addr;
       assembly {
           addr := create2(0, add(code, 0x20), mload(code), salt)
           if iszero(extcodesize(addr)) {
               revert(0, 0)
          }

       }
       return addr;
  }

  // prefix in main net is 0x41, testnet config is 0xa0
  function get(bytes1 prefix, bytes calldata code, uint256 salt) external view returns(address) {
      //bytes32 hash = keccak256(abi.encodePacked
      //(bytes1(0x41),address(this), salt, keccak256(code)));
      bytes32 hash = keccak256(abi.encodePacked(prefix,address(this), salt, keccak256(code)));
      address addr = address(uint160(uint256(hash)));
      return addr;
  }

}
 */
  @Test
  public void create2AddressTest() throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    manager.getDynamicPropertiesStore().saveAllowTvmTransferTrc10(1);
    manager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    manager.getDynamicPropertiesStore().saveAllowTvmSolidity059(1);
    manager.getDynamicPropertiesStore().saveAllowTvmIstanbul(1);
    String contractName = "Factory_0";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String abi = "[]";

    String factoryCode = "608060405234801561001057600080fd5b50610372806100206000396000f3fe60806040"
        + "5234801561001057600080fd5b50600436106100365760003560e01c80635573b40f1461003b5780639c4ae"
        + "2d01461012a575b600080fd5b6100e86004803603606081101561005157600080fd5b8101908080357effff"
        + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19169060200190929190803590602"
        + "0019064010000000081111561009a57600080fd5b8201836020820111156100ac57600080fd5b8035906020"
        + "01918460018302840111640100000000831117156100ce57600080fd5b90919293919293908035906020019"
        + "092919050505061022f565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffff"
        + "ffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6101ed6004803603604"
        + "081101561014057600080fd5b810190808035906020019064010000000081111561015d57600080fd5b8201"
        + "8360208201111561016f57600080fd5b8035906020019184600183028401116401000000008311171561019"
        + "157600080fd5b91908080601f01602080910402602001604051908101604052809392919081815260200183"
        + "8380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019"
        + "0929190505050610319565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffff"
        + "ffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6000808530848787604"
        + "051808383808284378083019250505092505050604051809103902060405160200180857effffffffffffff"
        + "ffffffffffffffffffffffffffffffffffffffffffffffff19167efffffffffffffffffffffffffffffffff"
        + "fffffffffffffffffffffffffffff191681526001018473ffffffffffffffffffffffffffffffffffffffff"
        + "1673ffffffffffffffffffffffffffffffffffffffff1660601b81526014018381526020018281526020019"
        + "4505050505060405160208183030381529060405280519060200120905060008160001c9050809250505094"
        + "9350505050565b600080828451602086016000f59050803b61033357600080fd5b809150509291505056fea"
        + "265627a7a7231582002325a8c6673dcf7a05095c37376c5f854f18b293680742606b903e7a2167ec664736f"
        + "6c63430005110032";


    String testCode = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57"
        + "600080fd5b5060d7806100396000396000f3fe608060405260043610602c5760003560e01c63ffffffff1680"
        + "6368e5c066146031578063e5aa3d5814606d575b600080fd5b348015603c57600080fd5b50d3801560485760"
        + "0080fd5b50d28015605457600080fd5b50605b6097565b60408051918252519081900360200190f35b348015"
        + "607857600080fd5b50d38015608457600080fd5b50d28015609057600080fd5b50605b60a5565b6000805460"
        + "01019081905590565b6000548156fea165627a7a72305820c637cddbfa24b6530000f2e54d90e0f6c1590783"
        + "5674109287f64303446f9afb0029";

    long value = 0;
    long fee = 1000000000;
    long consumeUserResourcePercent = 0;
    String methodDeploy = "deploy(bytes,uint256)";

    // deploy contract
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, abi, factoryCode, value, fee, consumeUserResourcePercent,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: deploy(bytes,uint256)
    long salt = 100L;
    String hexInput = AbiUtil.parseMethod(methodDeploy, Arrays.asList(testCode, salt));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    byte[] actualContract = convertToTronAddress(Arrays.copyOfRange(returnValue,
        12, 32));
    byte[] expectedContract =
        generateContractAddress2(factoryAddress,
            new DataWord(salt).getData(), Hex.decode(testCode));
    // check deployed contract
    Assert.assertEquals(actualContract, expectedContract);

    // trigger get function in smart contract and compare the actual
    // contract address with the value
    // computed in contract
    String methodToTrigger = "get(bytes1,bytes,uint256)";
    hexInput = AbiUtil.parseMethod(methodToTrigger,
        Arrays.asList(Wallet.getAddressPreFixString(), testCode, salt));
    // same input
    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertEquals(result.getRuntime().getResult().getHReturn(),
        new stest.tron.wallet.common.client.utils.DataWord(
            new DataWord(actualContract).getLast20Bytes()).getData());

    String ownerAddress2 = Wallet.getAddressPreFixString()
        + "8dcd6d3b585e41863123af20e57ec9f678035d92";
    rootDeposit.createAccount(Hex.decode(ownerAddress2), AccountType.Normal);
    rootDeposit.addBalance(Hex.decode(ownerAddress2), 30000000000000L);
    rootDeposit.commit();

    // deploy contract by OTHER user again, should fail
    hexInput = AbiUtil.parseMethod(methodDeploy, Arrays.asList(testCode, salt));
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(ownerAddress2),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNotNull(result.getRuntime().getRuntimeError());
    Assert.assertTrue(result.getRuntime().getResult().getException()
        instanceof OutOfEnergyException);
  }


}
