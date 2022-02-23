pragma experimental ABIEncoderV2;
contract Demo {
    function testPure(bytes32 hash, bytes[] memory signatures, address[] memory addresses) pure public returns(bytes32){
        return batchvalidatesign(hash, signatures, addresses);
    }

    function testArray(bytes32 hash, bytes[] memory signatures, address[] memory addresses) public returns(bytes32){
        return batchvalidatesign(hash, signatures, addresses);
    }
}