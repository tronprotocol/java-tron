 pragma solidity ^0.4.24;
contract SubC {
event log(string);

function () payable {}

function receiveToken() payable {}

function getBalance() constant public returns (uint256 r){
r = address(this).balance;
}
}
contract UseDot {
constructor() payable public{}
function() payable public{}
mapping(address => mapping(trcToken => uint256)) sender_tokens;

function trigger1(address addr) payable public {
 //SubC(addr).call.value(1000).tokenId(0x6e6d62)(bytes4(sha3("receiveToken()"))); // ERROR
}

function trigger2(address addr) payable public {
 //addr.transferToken.value(10)(10, 0x6e6d62); // ERROR
}

function trigger3(address addr) payable public {
// SubC(addr).receiveToken.tokenvalue(10)(); // ERROR
}

function trigger4(address addr) payable public {
// SubC(addr).receiveToken.tokenId(0x6e6d62)(); // ERROR
}

function trigger5(address addr) payable public {
SubC(addr).receiveToken.value(10)();
}
function trigger6(address addr, trcToken tokenId) payable public {
SubC(addr).call.value(1000)(bytes4(sha3("transferToken(uint256, trcToken)")), 10, tokenId);
}

function trigger7(address addr) payable public {
 //sender_tokens[msg.sender][msg.tokenid] += msg.tokenvalue; // compile success, no necessary to trigger
}
function trigger8(address addr) public payable returns(bytes r){
 //r = msg.data; // compile success, no necessary to trigger
}
function getBalance() public returns (uint256 r){
r = address(this).balance;
}
}