pragma solidity >0.5.0;
contract byteContract{
bytes b;
function testBytesGet(uint i) public returns (bytes1){
b = new bytes(3);
b[0]=0x0b;
b[1]=0x0c;
b[2]=0x0d;
return b[i];
}
}