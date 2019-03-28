pragma solidity ^0.4.0;

contract MathedFeed {

    function divideMathed() returns (uint ret) {
        uint x=1;
        uint y=0;
        return x/y;
    }
}


contract MathedUseContract {

    function MathedUse(address addr) returns (uint) {
        return MathedFeed(addr).divideMathed();
    }
}