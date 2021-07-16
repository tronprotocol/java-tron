
contract IllegalDecorate {
event log(uint256);
constructor() payable public{}
fallback() payable external{}
function transferTokenWithOutPayable(address payable toAddress, uint256 tokenValue) public {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);
}
}