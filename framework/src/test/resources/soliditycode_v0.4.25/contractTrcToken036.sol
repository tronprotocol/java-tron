pragma solidity ^0.4.24;
contract IllegalDecorate {
constructor() payable public{}
function() payable public{}
event log(uint256);
function transferTokenWithPure(address toAddress, uint256 tokenValue) public payable {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);
}
}

contract IllegalDecorate1 {
constructor() payable public{}
function() payable public{}
event log(uint256);
function transferTokenWithConstant(address toAddress, uint256 tokenValue) public constant {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);
}
}

contract IllegalDecorate2 {
constructor() payable public{}
function() payable public{}
event log(uint256);
function transferTokenWithView(address toAddress, uint256 tokenValue) public view {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);
}
}

contract IllegalDecorate3 {
event log(uint256);
constructor() payable public{}
function() payable public{}
function transferTokenWithOutPayable(address toAddress, uint256 tokenValue) public {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);
}
}

contract IllegalDecorate4 {
constructor() payable public{}
function() payable public{}
event log(uint256);
function transferTokenWithPure(address toAddress, uint256 tokenValue) public pure {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);
}
}