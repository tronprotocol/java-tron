//pragma solidity ^0.4.0;
contract InfoFeed {
function info() public payable returns (uint ret) { return 42; }
}
contract Consumer {
constructor() payable public{}
InfoFeed feed;
function setFeed(address addr) public { feed = InfoFeed(addr); }
function callFeed() public payable { feed.info.value(10).gas(800)(); }
}