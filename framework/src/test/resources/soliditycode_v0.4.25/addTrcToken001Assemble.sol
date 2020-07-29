//pragma solidity ^0.4.24;

contract InAssemble {

mapping(trcToken => uint256) tokenCnt;
mapping(uint256 => mapping(trcToken => trcToken)) cntTokenToken;
constructor () payable {}
function getBalance(address addr) constant returns(uint256 r) {
assembly{
r := balance(addr)
}
}

function getTokenBalanceConstant(address addr, trcToken tokenId) constant returns(uint256 r) {
assembly{
r := tokenbalance(tokenId, addr)
}
}

function getTokenBalance(address addr, trcToken tokenId) returns(uint256 r) {
assembly{
r := tokenbalance(tokenId, addr)
}
}

function transferTokenInAssembly(address addr, trcToken tokenId, uint256 tokenValue) payable {
bytes4 sig = bytes4(keccak256("()")); // function signature

assembly {
let x := mload(0x40) // get empty storage location
mstore(x,sig) // 4 bytes - place signature in empty storage

let ret := calltoken(gas, addr, tokenValue, tokenId,
x, // input
0x04, // input size = 4 bytes
x, // output stored at input location, save space
0x0 // output size = 0 bytes
)

// let ret := calltoken(gas, addr, tokenValue,
//   x, // input
//   0x04, // input size = 4 bytes
//   x, // output stored at input location, save space
//   0x0 // output size = 0 bytes
//   ) // ERROR


mstore(0x40, add(x,0x20)) // update free memory pointer
}

// assembly {
// let x := mload(0x40) //Find empty storage location using "free memory pointer"
// mstore(x,sig) //Place signature at begining of empty storage
// mstore(add(x,0x04),a) //Place first argument directly next to signature
// mstore(add(x,0x24),b) //Place second argument next to first, padded to 32 bytes

// let success := call( //This is the critical change (Pop the top stack value)
// 5000, //5k gas
// addr, //To addr
// 0, //No value
// x, /Inputs are stored at location x
// 0x44, //Inputs are 68 bytes long
// x, //Store output over input (saves space)
// 0x20) //Outputs are 32 bytes long

// c := mload(x) //Assign output value to c
// mstore(0x40,add(x,0x44)) // Set storage pointer to empty space
// }

}

function trcTokenInMap(trcToken tokenId, uint256 tokenValue) returns(uint256 r) {
tokenCnt[tokenId] += tokenValue;
r = tokenCnt[tokenId];
}

function cntTokenTokenInMap(trcToken tokenId1, trcToken tokenId2, uint256 tokenValue) returns(trcToken r) {
cntTokenToken[tokenValue][tokenId1] = tokenId2;
r = cntTokenToken[tokenValue][tokenId1];
}
}