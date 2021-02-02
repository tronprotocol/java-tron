//pragma solidity ^0.4.0;

contract MessageFeed {

    function mValue() payable public returns (uint ret) {
        return msg.value;
    }
}

contract MessageUseContract {
     function inputValue() payable public returns (uint){
        return msg.value;
    }
    function messageUse(address addr) payable public returns (uint) {
        return MessageFeed(addr).mValue.value(1)();
    }
}