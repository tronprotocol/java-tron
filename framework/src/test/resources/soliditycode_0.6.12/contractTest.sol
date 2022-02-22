

contract Test{

function a() public returns (uint){

uint256 count = 0;

for (uint256 i = 1; i > 0; i++) {

count++;

}

return count;

}

}