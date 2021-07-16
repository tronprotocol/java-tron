

contract testConstantContract{
function testPure(uint256 x,uint256 y) public pure returns (uint256 z) {
uint256 i=1;
return i + x + y;
}
}