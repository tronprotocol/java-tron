//pragma solidity ^0.4.24;
contract IllegalDecorate {
constructor() payable public{}
function() payable external{}
event log(uint256);
function transferTokenWithPure(address payable toAddress, uint256 tokenValue) public payable {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);
}
}

contract IllegalDecorate1 {
constructor() payable public{}
function() payable external{}
event log(uint256);
function transferTokenWithConstant(address payable toAddress, uint256 tokenValue) public payable {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);
}
}

contract IllegalDecorate2 {
constructor() payable public{}
function() payable external{}
event log(uint256);
function transferTokenWithView(address payable toAddress, uint256 tokenValue) public payable {
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
function() payable external{}
function transferTokenWithOutPayable(address payable toAddress, uint256 tokenValue) public {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);
}
}