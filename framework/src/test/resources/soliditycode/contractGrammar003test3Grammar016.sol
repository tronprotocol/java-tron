//pragma solidity ^0.4.0;
contract C {
uint private data;
function f(uint a) private returns(uint b) { return a + 1; }
function setData(uint a) public { data = a; }
function getData() public returns(uint) { return data; }
function compute(uint a, uint b) internal returns (uint) { return a+b; }
}
contract D {
function readData() public{
C c = new C();
//uint local = c.f(7); // error: member "f" is not visible
c.setData(3);
uint local = c.getData();
//  local = c.compute(3, 5); // error: member "compute" is not visible
}
}
contract E is C {
function g() public {
C c = new C();
uint val = compute(3, 5); // access to internal member (from derived to parent contract)
}
}
