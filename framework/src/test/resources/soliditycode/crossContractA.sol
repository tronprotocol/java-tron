
contract crossContractA{
uint256 public a = 0;
function increment(address from, address to, uint256 amount) external {
a = a + amount;
}
function read() public view returns (uint256) {
return a;
}
}