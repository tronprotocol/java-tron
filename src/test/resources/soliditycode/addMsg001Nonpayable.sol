//pragma solidity ^0.4.24;

contract IllegalDecorate {

event log(uint256);
constructor() payable public{}

function() payable external{}

function transferTokenWithOutPayable(address payable toAddress, uint256 tokenValue)public {
// function transferTokenWithValue(address toAddress, uint256 tokenValue) payable public {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);

}

}