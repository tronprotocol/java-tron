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
    function set() payable public {
       i=5;
    }
    function testSuicideNonexistentTarget(address payable nonexistentTarget) payable public {
         selfdestruct(nonexistentTarget);
    }
}