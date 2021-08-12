


contract  getterContract {

constructor() public payable{}
fallback() external payable{}

uint public c = msg.value;

function getDataUsingAccessor() public payable returns (uint){

return c;

}

}