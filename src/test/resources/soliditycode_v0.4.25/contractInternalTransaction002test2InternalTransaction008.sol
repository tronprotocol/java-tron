pragma solidity ^0.4.24;

contract A{
    constructor() payable public{}
    function() payable public{}

    function testAssert(address bAddress,uint256 amount) public payable{
        bAddress.call.value(amount).gas(1000000)(bytes4(keccak256("callCGetZero(bool)")),false);//2.1
        bAddress.call.value(amount).gas(1000000)(bytes4(keccak256("callCGetZero(bool)")),true);
    }
    function testRequire(address cAddress,uint256 amount) public payable{
        cAddress.call.value(amount).gas(1000000)(bytes4(keccak256("newBAndTransfer(bool)")),false);//2.1
        cAddress.call.value(amount).gas(1000000)(bytes4(keccak256("newBAndTransfer(bool)")),true);
    }
    function testAssert1(address bAddress,uint256 amount) public payable{
        bAddress.call.value(amount).gas(1000000)(bytes4(keccak256("callCGetZero(bool)")),true);
        bAddress.call.value(amount).gas(1000000)(bytes4(keccak256("callCGetZero(bool)")),false);//2.1
    }
    function testtRequire2(address cAddress,uint256 amount) public payable{
        cAddress.call.value(amount).gas(1000000)(bytes4(keccak256("newBAndTransfer(bool)")),true);
        cAddress.call.value(amount).gas(1000000)(bytes4(keccak256("newBAndTransfer(bool)")),false);//2.1
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