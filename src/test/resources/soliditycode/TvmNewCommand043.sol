contract TestBitwiseShift {

     function shlTest(int256 num, int256 input) public  returns (bytes32 out) {
         assembly {
                 out := shl(num, input)
             }
     }
     function shrTest(int256 num, int256 input) public  returns (bytes32 out) {
         assembly {
                 out := shr(num, input)
             }
     }
     function sarTest(int256 num, int256 input) public  returns (bytes32 out) {
         assembly {
                 out := sar(num, input)
             }
     }
 }