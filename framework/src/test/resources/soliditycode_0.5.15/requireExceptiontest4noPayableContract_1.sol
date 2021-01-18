//pragma solidity ^0.4.0;

contract noPayableContract {

function noPayable() public  returns (uint){
return msg.value;
}
}