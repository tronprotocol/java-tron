pragma solidity ^0.4.0;
contract byteContract {
bytes b;
function testBytesGet(uint i) returns (bytes1){
b = new bytes(3);
b[0]=12;
b[1]=13;
b[2]=14;
return b[i];
}
}