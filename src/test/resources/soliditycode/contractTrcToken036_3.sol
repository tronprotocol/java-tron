//pragma solidity ^0.4.24;
contract IllegalDecorate {
constructor() payable public{}
function() payable external{}
event log(uint256);
function transferTokenWithView(address payable toAddress, uint256 tokenValue) public view {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);
}
}