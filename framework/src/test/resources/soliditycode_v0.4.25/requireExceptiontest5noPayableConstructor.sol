pragma solidity ^0.4.0;

contract MyContract {
    uint money;

    function MyContract(uint _money) {
        require(msg.value >= _money);
        money = _money;
    }
}