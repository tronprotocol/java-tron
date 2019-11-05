pragma solidity ^0.4.24;

contract A{
    constructor() payable public{}
    function() payable public{}
    function test1() public payable{
        B b1 = (new B).value(10)();//1.1
        b1.callCGetZero(true);//1.4
        b1.callCGetZero(false);
    }
    function test2() public payable{
        C c1 = (new C).value(10)();//1.1
        c1.newBAndTransfer(true);//1.4
        c1.newBAndTransfer(false);

    }
    function getBalance() view public returns(uint256){
        return this.balance;
    }
}

contract B{
    constructor() payable public{}
    function() payable public{}
    function getOne() payable returns(uint256){
        return 1;
    }
    function callCGetZero(bool success) payable{
        if(!success){
            assert(1==2);
        }
    }
    function getBalance() view public returns(uint256){
        return this.balance;
    }
}

contract C{
    uint256 public flag=0;
    constructor() payable public{}
    function() payable public{}
    function getZero() payable public returns(uint256){
        return 0;
    }
    function newBAndTransfer(bool success) payable public returns(uint256){
        flag = 1;
        if(!success){
        require(2==1);
        }
    }
    function getFlag() public returns(uint256){
        return flag;
    }
}