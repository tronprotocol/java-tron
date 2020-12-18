

contract testConstantContract{
 uint256 public i;
 function testNoPayable() public  returns (uint256 z) {
 i=1;
 z=i;
 return z;
 }
 }