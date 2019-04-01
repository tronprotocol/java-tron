//pragma solidity ^0.4.0;

contract  findArgsContractTest{
    function findArgsByIndexTest(uint i) public returns (uint z) {
        uint[] memory a = new uint[](3);
        a[0]=1;
        a[1]=2;
        a[2]=3;
        return a[i];
    }
}
