pragma solidity ^0.4.0;

contract noPayableContract {

function noPayable() returns (uint){
return msg.value;
}
}