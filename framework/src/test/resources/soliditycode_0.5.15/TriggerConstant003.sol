//pragma solidity ^0.4.0;

contract testConstantContract{
    function testView() public view returns (uint256 z) {
        uint256 i=1;
        return i;
    }

    function testPure() public pure returns (uint256 z) {
        uint256 i=1;
        return i;
    }

    function testPayable() public payable returns (uint256 z) {
        uint256 i=1;
        return i;
    }
}