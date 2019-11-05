//pragma solidity ^0.4.0;

contract Dest {
    event logFallback(uint256 indexed, uint256 indexed, uint256 indexed);
 event logGetToken(uint256 indexed, uint256 indexed, uint256 indexed, uint256);


 constructor() payable public {}

    function getToken(trcToken tokenId) payable public{
      emit  logGetToken(msg.sender.tokenBalance(tokenId), msg.tokenid, msg.tokenvalue, msg.value);
 }

    function () payable external{
      emit  logFallback(msg.tokenid, msg.tokenvalue, msg.value);
 }
}