contract callerContract {
    constructor() payable public{}
    fallback() payable external{}
    function delegateCallCreate2(address called_address, bytes memory code, uint256 salt) public {
       called_address.delegatecall(abi.encodeWithSignature("deploy(bytes,uint256)",code,salt));
    }
    function callCreate2(address called_address,bytes memory code, uint256 salt) public returns(bool,bytes memory){
       return called_address.call(abi.encodeWithSignature("deploy(bytes,uint256)",code,salt));
    }
}


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
    uint public i;
    constructor () public {
    }
    function plusOne() public returns(uint){
        i++;
        return i;
    }
}