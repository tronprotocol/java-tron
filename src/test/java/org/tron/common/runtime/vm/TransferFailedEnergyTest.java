package org.tron.common.runtime.vm;

public class TransferFailedEnergyTest extends VMTestBase {
/*

contract EnergyOfTransferFailedTest {
    function testTransferTrxInsufficientBalance() payable public{
        msg.sender.transfer(10);
    }

    function testSendTrxInsufficientBalance() payable public{
        msg.sender.send(10);
    }

    function testTransferTokenInsufficientBalance(trcToken tokenId) payable public{
        msg.sender.transferToken(10, tokenId);
    }

    function testCallTrxInsufficientBalance() public {
        reciver.call.value(10)(abi.encodeWithSignature("test()"));

    }

    function testCreateTrxInsufficientBalance(address payable reciver) public {
        reciver.call.value(10)(abi.encodeWithSignature("test()"));
                (new reciver).value(10)();
    }

    function testDelegatecallInsufficientBalance(address payabale reciver) public {
        reciver.delegatecall.value(10)(abi.encodeWithSignature("test()"));
    }

    //
    function testTransferTrxNonexistentTarget(address payable nonexistentTarget) payable public {
        require(this.balance >= 10);
        nonexistentTarget.transfer(10);
    }

    function testTransferTokenNonexistentTarget(address payable nonexistentTarget, trcToken tokenId) payable public {
        require(this.balance >= 10);
        nonexistentTarget.transferToken(10, tokenId);
    }

    function testCallTrxNonexistentTarget(address payable nonexistentTarget) public {
        nonexistentTarget.call.value(10)(abi.encodeWithSignature("test()"));

    }

    function testDelegatecallNonexistentTarget(address payable nonexistentTarget) public {
        nonexistentTarget.delegatecall.value(10)(abi.encodeWithSignature("test()"));
    }

    function testTransferTrxSelf() payable public{
        address(this).transfer(10);
    }

    function testSendTrxSelf() payable public{
        address(this).send(10);
    }

    function testTransferTokenSelf(trcToken tokenId) payable public{
        address(this).transferToken(10, tokenId);
    }


    function testSuicide(address payable nonexistentTarget) {
         suicide(nonexistentTarget);
    }
}



contract Reciver {
    function test() payable public {}
}
 */


}
