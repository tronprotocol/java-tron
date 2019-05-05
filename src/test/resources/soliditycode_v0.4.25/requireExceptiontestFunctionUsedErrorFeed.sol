pragma solidity ^0.4.0;

contract MessageFeed {

    function mValue() payable returns (uint ret) {
        return msg.value;
    }
}

contract MessageUseContract {
     function inputValue() payable returns (uint){
        return msg.value;
    }
    function messageUse(address addr) payable returns (uint) {
        return MessageFeed(addr).mValue.value(1)();
    }
}