//pragma solidity ^0.4.0;

contract testConstantContract{
    uint256 public i;
    function testPayable() public payable returns (uint256 z) {
       i=1;
       z=i;
       return z;
    }
    function testNoPayable() public  returns (uint256 z) {
       i=1;
       z=i;
       return z;
    }
    function testView() public view returns (uint256 z) {
       uint256 i=1;
       return i;
    }
    function testPure() public pure returns (uint256 z) {
       uint256 i=1;
       return i;
    }
    function testView2() public view returns (uint256 z) {
       uint256 i=1;
       revert();
       return i;
    }
}