//pragma solidity ^0.4.0;

contract testConstantContract{
function testView() public constant returns (uint256 z) {
uint256 i=1;
return i;
}
}