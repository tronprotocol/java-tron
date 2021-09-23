

contract noPayableContract {

function noPayable() public  returns (uint){
return msg.value;
}
}