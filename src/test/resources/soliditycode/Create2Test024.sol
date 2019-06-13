contract Factory {
    event Deployed(address addr, uint256 salt, address sender);
    function deploy(bytes memory code, uint256 salt) public returns(address){
        TestConstract addr;
                TestConstract addr1;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }

        addr.testSuicideNonexistentTarget(msg.sender);
        addr.set();
        emit Deployed(address(addr), salt, msg.sender);
        return address(addr);
    }

    function deploy2(bytes memory code, uint256 salt) public returns(address){
            TestConstract addr;
                    TestConstract addr1;
            assembly {
                addr := create2(0, add(code, 0x20), mload(code), salt)
                if iszero(extcodesize(addr)) {
                    revert(0, 0)
                }
            }

            //addr.testSuicideNonexistentTarget(msg.sender);
            //addr.set();

            assembly {
                addr1 := create2(0, add(code, 0x20), mload(code), salt)
                if iszero(extcodesize(addr)) {
                    revert(0, 0)
                }
            }
            emit Deployed(address(addr), salt, msg.sender);
            return address(addr);
    }
}



contract TestConstract {
    uint public i=1;
    constructor () public {
    }

    function set() public{
        i=9;
    }
    function testSuicideNonexistentTarget(address payable nonexistentTarget) payable public {
         selfdestruct(nonexistentTarget);
    }
}