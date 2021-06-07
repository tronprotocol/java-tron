

contract InAssemble {

mapping(trcToken => uint256) tokenCnt;
mapping(uint256 => mapping(trcToken => trcToken)) cntTokenToken;
constructor () payable public {}
function getBalance (address addr) view public returns(uint256 r) {
assembly{
r := balance(addr)
}
}

function getTokenBalanceConstant  (address addr, trcToken tokenId) view public returns(uint256 r) {
assembly{
r := tokenbalance(tokenId, addr)
}
}

function getTokenBalance (address addr, trcToken tokenId) public returns(uint256 r) {
assembly{
r := tokenbalance(tokenId, addr)
}
}

function transferTokenInAssembly(address addr, trcToken tokenId, uint256 tokenValue) public payable {
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

}

function trcTokenInMap(trcToken tokenId, uint256 tokenValue) public returns(uint256 r) {
tokenCnt[tokenId] += tokenValue;
r = tokenCnt[tokenId];
}

function cntTokenTokenInMap(trcToken tokenId1, trcToken tokenId2, uint256 tokenValue) public returns(trcToken r) {
cntTokenToken[tokenValue][tokenId1] = tokenId2;
r = cntTokenToken[tokenValue][tokenId1];
}
}