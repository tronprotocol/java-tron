pragma solidity ^0.4.0;

contract Account{
    uint256 public accId;

    function Account(uint accountId) payable{
        accId = accountId;
    }
}

contract Initialize{
    // Account public account = new Account(10);

    function newAccount(){
        Account account = new Account(1);
    }

}