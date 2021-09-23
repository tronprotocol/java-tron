pragma solidity ^0.4.4;
contract Counter {
uint count = 0;
address owner;
function Counter() {
owner = msg.sender;
}
function increment() public {
uint step = 10;
if (owner == msg.sender) {
count = count + step;
}
}
function getCount() public returns (uint) {
return count;
}
function kill() {
if (owner == msg.sender) {
selfdestruct(owner);
}
}
}