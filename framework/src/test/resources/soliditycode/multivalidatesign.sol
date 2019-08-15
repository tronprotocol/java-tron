pragma experimental ABIEncoderV2;
contract Demo {
    function testArray(bytes32 hash, bytes[] memory signatures, address[] memory addresses) pure public returns(uint){
        if (multivalidatesign(hash, signatures, addresses)) {
            return 1;
        }else {
            return 2;
        }
    }

    function testArray2(bytes memory data) public returns(bool, bytes memory){
        return address(0x9).delegatecall(data);
    }

    function testArray4(bytes memory data) public {
        //address(0x1).delegatecall(data);
    }
    //function testArray3(bytes32 hash, bytes[] memory signatures, address[] memory addresses) public {
        //address(0x9).delegatecall(hash,signatures,addresses);
    //}
}