//pragma solidity ^0.4.0;


contract getterContract{

uint public c = msg.value;

function getDataUsingAccessor() returns (uint){

return c;

}

}