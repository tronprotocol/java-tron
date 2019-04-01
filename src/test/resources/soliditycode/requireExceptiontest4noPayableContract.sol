//pragma solidity ^0.4.0;

contract noPayableContract {

function noPayable() public payable returns (uint){
return msg.value;
}
}