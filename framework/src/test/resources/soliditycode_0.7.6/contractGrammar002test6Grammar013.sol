
contract Counter {
uint count = 0;
address payable owner;
//function Counter() public{
constructor() public{
owner = msg.sender;
}
function increment() public {
uint step = 10;
if (owner == msg.sender) {
count = count + step;
}
}
function getCount() public returns (uint){
return count;
}
function kill() public{
if (owner == msg.sender) {
selfdestruct(owner);
//selfdestruct(address(owner));
}
}
}