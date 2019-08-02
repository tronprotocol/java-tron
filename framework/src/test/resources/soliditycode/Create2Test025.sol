contract Factory {
    event Deployed(address addr, uint256 salt, address sender);
    constructor() public {
    }

    function create2(bytes memory code, uint256 salt) public returns(address){
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

contract TestContract{
    uint256 public num;
    constructor(uint256 j) public{
        num = j;
    }
    function getNum() public returns (uint256){
          return num;
    }
}