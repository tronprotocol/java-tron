//pragma solidity ^0.4.24;

contract IllegalDecorate {

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