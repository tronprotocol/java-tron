//pragma solidity ^0.4.0;

contract testConstantContract{
uint256 public i;
function testPayable() public payable returns (uint256 z) {
i=1;
z=i;
return z;
}
}