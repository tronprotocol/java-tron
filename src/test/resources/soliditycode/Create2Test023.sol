contract factory {
    constructor() payable public {
    }

    function deploy(bytes memory code, uint256 salt) public returns(address){
        Caller addr;
        assembly {
           addr := create2(0, add(code, 0x20), mload(code), salt)
           if iszero(extcodesize(addr)) {
               revert(0, 0)
           }
        }
        return address(addr);
    }

    function testCreate() payable public returns (address){
        Caller add = (new Caller).value(0)();
        return address(add);
    }

    function kill(  ) payable public{
        selfdestruct(msg.sender);
    }
}



contract Caller {
    constructor() payable public {}
    function test() payable public returns (uint256){return 1;}
}