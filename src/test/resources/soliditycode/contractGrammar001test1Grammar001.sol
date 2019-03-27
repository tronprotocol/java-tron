pragma solidity ^0.4.16;
contract FunctionSelector {
 function select(bool useB, uint x) returns (uint z) {
 var f = a;
 if (useB) f = b;
 return f(x);
 }
function a(uint x) returns (uint z) {
 return x * x;
 }
function b(uint x) returns (uint z) {
 return 2 * x;
 }
}