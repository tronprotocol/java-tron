contract TestBitwiseShift {

     function shlTest(uint256 num, uint256 input) public  returns (bytes32 out) {
         assembly {
                 out := shl(num, input)
             }
     }
     function shrTest(uint256 num, uint256 input) public  returns (bytes32 out) {
         assembly {
                 out := shr(num, input)
             }
     }
     function sarTest(uint256 num, uint256 input) public  returns (bytes32 out) {
         assembly {
                 out := sar(num, input)
             }
     }
 }