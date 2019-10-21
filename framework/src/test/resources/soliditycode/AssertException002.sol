//pragma solidity ^0.4.0;

contract AssertException{
  function divideIHaveArgsReturn(int x,int y) public returns (int z) {
    return  x / y;
  }
  function testAssert() public {
    require(2==1);
  }
}
contract C {
    constructor() public payable {
       assert(1==2);
    }
    function fun() public {
    }
}