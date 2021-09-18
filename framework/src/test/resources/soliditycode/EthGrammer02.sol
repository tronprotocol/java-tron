contract D {
    constructor() public payable{}

    event createAddress(address addr);
    function createDeployEf(bytes memory code) public returns(address addr){
        address addr;
        assembly {
            addr := create(0, add(code, 0x20), mload(code))
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        return addr;
    }

    function create2DeployEf(bytes memory code,uint256 salt) public returns(address addr){
        address addr;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        emit createAddress(addr);
        return addr;
    }

    function setSlot(bytes memory slot,uint256 value) external {
//        uint256 value = 123;
        assembly {
            sstore(slot, value)
        }
    }

    function getSlot(bytes memory slot) view external returns(uint res) {
        assembly {
            res := sload(slot)
        }
    }
}





