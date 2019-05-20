//pragma solidity ^0.4.0;

contract Dest {
    event logFallback(uint256 indexed, uint256 indexed, uint256 indexed);
 event logGetToken(uint256 indexed, uint256 indexed, uint256 indexed, uint256);


 function Dest() payable public {}

    function getToken(trcToken tokenId) payable {
        logGetToken(msg.sender.tokenBalance(tokenId), msg.tokenid, msg.tokenvalue, msg.value);
 }

    function () payable {
        logFallback(msg.tokenid, msg.tokenvalue, msg.value);
 }
}