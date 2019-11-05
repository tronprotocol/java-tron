contract Factory {
    event Deployed(address addr, trcToken salt, address sender);
    event Deployed1(address addr, uint8 salt, address sender);
    event Deployed2(address addr, address salt, address sender);
    event Deployed3(address addr, string salt, address sender);


    function deploy(bytes memory code, trcToken salt) public returns(address){
        address addr;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        emit Deployed(addr, salt, msg.sender);
        return addr;
    }

    function deploy1(bytes memory code, uint8 salt) public returns(address){
            address addr;
            assembly {
                addr := create2(0, add(code, 0x20), mload(code), salt)
                if iszero(extcodesize(addr)) {
                    revert(0, 0)
                }
            }
            emit Deployed1(addr, salt, msg.sender);
            return addr;
    }

    function deploy2(bytes memory code, address salt) public returns(address){
            address addr;
            assembly {
                addr := create2(0, add(code, 0x20), mload(code), salt)
                if iszero(extcodesize(addr)) {
                    revert(0, 0)
                }
            }
            emit Deployed2(addr, salt, msg.sender);
            return addr;
    }

    function deploy3(bytes memory code, string memory salt) public returns(address){
            address addr;
            assembly {
                addr := create2(0, add(code, 0x20), mload(code), salt)
                if iszero(extcodesize(addr)) {
                    revert(0, 0)
                }
            }
            emit Deployed3(addr, salt, msg.sender);
            return addr;
    }

}


contract TestConstract {
    uint public i=1;
    function testTransfer(uint256 i) payable public{
          msg.sender.transfer(i);
    }
    function testTransferToken(uint256 i,trcToken tokenId) payable public{
          msg.sender.transferToken(i, tokenId);
    }
    function testSuicideNonexistentTarget(address payable nonexistentTarget) payable public {
         selfdestruct(nonexistentTarget);
    }
}

contract TestConstract1 {
    uint public i=2;
    function testTransfer(uint256 i) payable public{
          msg.sender.transfer(i);
    }
    function testTransferToken(uint256 i,trcToken tokenId) payable public{
          msg.sender.transferToken(i, tokenId);
    }
    function testSuicideNonexistentTarget(address payable nonexistentTarget) payable public {
         selfdestruct(nonexistentTarget);
    }
}

contract TestConstract2 {
    uint public i=3;
    function testTransfer(uint256 i) payable public{
          msg.sender.transfer(i);
    }
    function testTransferToken(uint256 i,trcToken tokenId) payable public{
          msg.sender.transferToken(i, tokenId);
    }
    function testSuicideNonexistentTarget(address payable nonexistentTarget) payable public {
         selfdestruct(nonexistentTarget);
    }
}

contract TestConstract3 {
    uint public i=4;
    function testTransfer(uint256 i) payable public{
          msg.sender.transfer(i);
    }
    function testTransferToken(uint256 i,trcToken tokenId) payable public{
          msg.sender.transferToken(i, tokenId);
    }
    function testSuicideNonexistentTarget(address payable nonexistentTarget) payable public {
         selfdestruct(nonexistentTarget);
    }
}