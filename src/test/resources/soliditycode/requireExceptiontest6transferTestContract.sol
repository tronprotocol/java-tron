pragma solidity ^0.4.0;

contract transferTestContract {
    function tranferTest(address addr) public payable{
        addr.transfer(10);

    }
}