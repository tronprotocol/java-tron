//pragma solidity ^0.4.0;

contract transferTestContract {
    function tranferTest(address payable addr) public payable{
        addr.transfer(10);

    }
}