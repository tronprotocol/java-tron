//pragma solidity ^0.4.24;
contract rTest {
function info() public payable returns (uint,address,bytes4,uint,uint,uint,address,uint) {
//function info() public payable returns (address ,uint,uint,uint,bytes32,uint,bytes,uint,address,bytes4,uint,uint,uint,address,uint) {
//var a = block.coinbase ;
//var b = block.difficulty;
//var c = block.gaslimit;
//var d = block.number;
//var e = block.blockhash(0);
//var e = d;
//var f = block.timestamp;
//bytes memory g = msg.data;
uint256 h = gasleft();
address payable i = msg.sender;
bytes4 j = msg.sig;
uint256 k = msg.value;
uint256 l = now;
uint256 m = tx.gasprice;
address payable n = tx.origin;
uint256 o = address(this).balance;
return (h,i,j,k,l,m,n,o);
//return (a,b,c,d,e,f,g,h,i,j,k,l,m,n,o);
}
}