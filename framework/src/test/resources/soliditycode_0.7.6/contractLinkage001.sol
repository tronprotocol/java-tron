

contract divideIHaveArgsReturnStorage{
constructor() payable public{}
fallback() payable external{}
function divideIHaveArgsReturn(int x,int y) public payable returns (int z) {
return z = x / y;
}
}