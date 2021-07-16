pragma experimental ABIEncoderV2;
contract multiValidateSignContract {
   function testArray(bytes32 hash, bytes[] memory signatures, address[] memory addresses) public returns(uint){
       if (multivalidatesign(hash, signatures, addresses)) {
           return 1;
       }else {
           return 2;
       }
   }
}