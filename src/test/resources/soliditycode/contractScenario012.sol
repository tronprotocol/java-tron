//pragma solidity ^0.4.0;
contract PayTest {

uint256 public n;
constructor() payable public{
n = 0;
}

function nPlusOne() public{
n = n+1;
}

//get current contract balance
function getBalance() payable public returns (uint) {
return address(this).balance;
}

function getSenderBalance() public view returns(address, uint) {
return (msg.sender, msg.sender.balance);
}

address public user;

//deposit 1 coin to msg.sender
function depositOneCoin() payable public returns(bool success){
return msg.sender.send(1);
}

// function transferOneCoin() payable public returns(){
// address(msg.sender).transfer(1);
// }

// function depositOneCoin() payable public returns(address addr, uint amount, bool success){
// return (msg.sender, msg.value, msg.sender.send(1));
// }

//deposit coin to msg.sender
function deposit(uint256 money) payable public returns(bool success){
return msg.sender.send(money);
}
// function deposit(uint money) payable public returns(address addr, uint amount, bool success){
// return (msg.sender, msg.value, msg.sender.send(money));
// }

// function () payable {
// msg.sender.send(1);
// }

function sendToAddress(address payable _receiver) payable public{
_receiver.transfer(msg.value);
}

function sendToAddress2(address payable _receiver) payable public{
_receiver.transfer(5);
}

}