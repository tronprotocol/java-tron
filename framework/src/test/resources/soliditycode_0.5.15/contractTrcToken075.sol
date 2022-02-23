//pragma solidity ^0.4.0;

contract Dest {
    event logFallback(uint256 indexed, uint256 indexed, uint256 indexed);
 event logGetToken(uint256 indexed, uint256 indexed, uint256 indexed, uint256);

 constructor() payable public {}

 function getToken(trcToken tokenId) payable public{
        emit logGetToken(msg.sender.tokenBalance(tokenId), msg.tokenid, msg.tokenvalue, msg.value);
 }

 function getTokenLongMin() payable public{
        // long.min - 1000020
 emit logGetToken(msg.sender.tokenBalance(trcToken(-9223372036855775828)), msg.tokenid, msg.tokenvalue, msg.value);
 }

 function getTokenLongMax() payable public{
        // long.max + 1000020
 emit logGetToken(msg.sender.tokenBalance(trcToken(9223372036855775827)), msg.tokenid, msg.tokenvalue, msg.value);
 }

 function () payable external{
      emit  logFallback(msg.tokenid, msg.tokenvalue, msg.value);
 }
}