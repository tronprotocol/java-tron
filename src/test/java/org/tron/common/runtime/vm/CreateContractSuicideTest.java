package org.tron.common.runtime.vm;


import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.TVMTestUtils;
import org.tron.common.storage.DepositImpl;
import org.tron.core.config.Parameter;
import org.tron.core.config.Parameter.ForkBlockVersionConsts;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class CreateContractSuicideTest extends  VMTestBase {

  /*

  pragma solidity ^0.4.24;

contract testA {
    constructor() public{
        A a = (new A).value(10)();
        a.fun();
    }
}

contract testB {
    constructor() public{
        B b = (new B).value(10)();
        b.fun();
    }
}


contract testC {
    constructor() public{
        C c = (new C).value(10)();
        c.fun();
    }
}


contract A {
    constructor() public payable{
        selfdestruct(msg.sender);
    }
    function fun() {
    }

}

contract B {
    constructor() public payable {
        revert();
    }
    function fun() {
    }
}


contract C {
    constructor() public payable {
       assert(1==2);
    }
    function fun() {
    }
}
   */

@Test
  public void testAAfterVersion3_5()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
    this.manager.getDynamicPropertiesStore().statsByVersion(ForkBlockVersionEnum.VERSION_3_5.getValue(), stats);


    String contractA = "testA";
    String contractB = "testB";
    byte[] address = Hex.decode(OWNER_ADDRESS);

    String aABI = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    String bABI = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";

    String aCode = "6080604052348015600f57600080fd5b50d38015601b57600080fd5b50d28015602757600080fd5b506000600a603260c3565b6040518091039082f080158015604c573d6000803e3d6000fd5b509050905080600160a060020a031663946644cd6040518163ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401600060405180830381600087803b15801560a757600080fd5b505af115801560ba573d6000803e3d6000fd5b505050505060d2565b60405160088061011583390190565b6035806100e06000396000f3006080604052600080fd00a165627a7a723058206423959ef5ac634ea94eb10f52616f80495e4e4aef1ed91319598fba644629090029608060405233ff00";
    String bCode = "6080604052348015600f57600080fd5b50d38015601b57600080fd5b50d28015602757600080fd5b506000600a603260c3565b6040518091039082f080158015604c573d6000803e3d6000fd5b509050905080600160a060020a031663946644cd6040518163ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401600060405180830381600087803b15801560a757600080fd5b505af115801560ba573d6000803e3d6000fd5b505050505060d2565b604051600a8061011583390190565b6035806100e06000396000f3006080604052600080fd00a165627a7a7230582062fa765223b845bf1212cbca633cd7d26386dca60c73eee477aad9d4b76f7cbe00296080604052600080fd00";

    long value = 0;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;
    long engeryLiimt = 100000000;

    Transaction aTrx = TVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractA, address, aABI, aCode, value, fee, consumeUserResourcePercent, null, engeryLiimt);
    Runtime runtime = TVMTestUtils.processTransactionAndReturnRuntime(aTrx, DepositImpl.createRoot(manager), null);
    Assert.assertNull(runtime.getRuntimeError());


  }

  public void testABefterVersion3_5() {

  }

  public void testBAfterVersion3_5() {

  }

  public void testBBeforeVersion3_5() {

  }







}
