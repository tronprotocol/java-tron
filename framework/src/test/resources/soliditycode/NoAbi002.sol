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



contract testNoABiContract {
    uint public i=0;
    event trigger(uint256 i, address sender);

    constructor () public {}

    function plusOne() public returns(uint){
        i++;
        emit trigger(i, msg.sender);
        return i;
    }
}