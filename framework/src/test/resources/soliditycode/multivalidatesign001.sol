pragma experimental ABIEncoderV2;
contract Demo {
    function testArray(bytes32 hash, bytes[] memory signatures, address[] memory addresses) pure public returns(uint){
            if (multivalidatesign(hash, signatures, addresses)) {
                return 1;
            }else {
                return 2;
            }
        }
}