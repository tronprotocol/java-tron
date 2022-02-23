

contract noPayableContract {

function noPayable() public payable returns (uint){
return msg.value;
}
}