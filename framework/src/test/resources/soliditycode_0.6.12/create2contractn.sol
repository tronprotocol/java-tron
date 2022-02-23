contract Factory {
    event Deployed(address addr, uint256 salt, address sender);
    function deploy(bytes memory code, uint256 salt) public returns(address){
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