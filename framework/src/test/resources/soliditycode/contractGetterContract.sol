//pragma solidity ^0.4.0;


contract  getterContract {

constructor() public payable{}
function() external payable{}

uint public c = msg.value;

function getDataUsingAccessor() public payable returns (uint){

return c;

}

}