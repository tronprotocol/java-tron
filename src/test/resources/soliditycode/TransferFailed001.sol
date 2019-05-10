contract EnergyOfTransferFailedTest {
    constructor() payable public {

    }
    // InsufficientBalance
    function testTransferTrxInsufficientBalance(uint256 i) payable public{
        msg.sender.transfer(i);
    }

    function testSendTrxInsufficientBalance(uint256 i) payable public{
        msg.sender.send(i);
    }

    function testTransferTokenInsufficientBalance(uint256 i,trcToken tokenId) payable public{
        msg.sender.transferToken(i, tokenId);
    }

    function testCallTrxInsufficientBalance(uint256 i,address payable caller) public {
        caller.call.value(i)(abi.encodeWithSignature("test()"));
    }

    function testCreateTrxInsufficientBalance(uint256 i) payable public {
        (new Caller).value(i)();
    }

    // NonexistentTarget

    function testSendTrxNonexistentTarget(uint256 i,address payable nonexistentTarget) payable public {
        nonexistentTarget.send(i);
    }

    function testTransferTrxNonexistentTarget(uint256 i,address payable nonexistentTarget) payable public {
        nonexistentTarget.transfer(i);
    }

    function testTransferTokenNonexistentTarget(uint256 i,address payable nonexistentTarget, trcToken tokenId) payable public {
        nonexistentTarget.transferToken(i, tokenId);
    }

    function testCallTrxNonexistentTarget(uint256 i,address payable nonexistentTarget) payable public {
        nonexistentTarget.call.value(i)(abi.encodeWithSignature("test()"));
    }

    function testSuicideNonexistentTarget(address payable nonexistentTarget) payable public {
         selfdestruct(nonexistentTarget);
    }

    // target is self
    function testTransferTrxSelf(uint256 i) payable public{
        address payable self = address(uint160(address(this)));
        self.transfer(i);
    }

    function testSendTrxSelf(uint256 i) payable public{
        address payable self = address(uint160(address(this)));
        self.send(i);
    }

    function testTransferTokenSelf(uint256 i,trcToken tokenId) payable public{
        address payable self = address(uint160(address(this)));
        self.transferToken(i, tokenId);
    }
}



contract Caller {
    constructor() payable public {}
    function test() payable public {}
}