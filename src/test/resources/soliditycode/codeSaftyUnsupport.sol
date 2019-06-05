//pragma solidity ^0.4.24;

contract SubC {

event log(string);

function () payable external{}

function receiveToken() payable public{}

function getBalance() view public returns (uint256 r) {
r = address(this).balance;
}
}

contract UseDot {
constructor() payable public{}
function() payable external{}
mapping(address => mapping(trcToken => uint256)) sender_tokens;

function trigger1(address payable addr, trcToken tokenInputId) payable public {
 //address(SubC(addr)).call.value(1000).tokenId(tokenInputId)(abi.encodeWithSignature("receiveToken()")); // ERROR
}

function trigger2(address payable addr) payable public {
// addr.transferToken.value(10)(10, 0x6e6d62); // ERROR
}

function trigger3(address payable addr) payable public {
 // address(SubC(addr)).receiveToken.tokenvalue(10)(); // ERROR
}

function trigger4(address payable addr) payable public {
 //SubC(addr).receiveToken.tokenId(0x6e6d62)(); // ERROR
}

function trigger5(address payable addr) payable public {
  SubC(addr).receiveToken.value(10)();
}

function trigger6(address payable addr, trcToken tokenId) payable public {
address(SubC(addr)).call.value(1000)(abi.encodeWithSignature("transferToken(uint256, trcToken)", 10, tokenId));
}

function trigger7(address addr) payable public {
 //sender_tokens[msg.sender][msg.tokenid] += msg.tokenvalue; // compile success, no necessary to trigger
}

function trigger8(address addr) public payable returns(bytes memory r){
// r = msg.data;  // compile success, no necessary to trigger
}

function getBalance() public returns (uint256 r){
r = address(this).balance;
}
}