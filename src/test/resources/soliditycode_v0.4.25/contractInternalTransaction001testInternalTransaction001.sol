pragma solidity ^0.4.24;

contract A{
    constructor() payable public{}
    function() payable public{}
    function test1(address cAddr) public payable{
        B b1 = (new B).value(10)();//1.1
        B b2 = new B();//1.2
        b2.transfer(5);//1.3
        b2.callCGetZero(cAddr, 1);//1.4
        b2.callCGetZero(cAddr,2);//1.6
    }
    function test2(address cAddress,uint256 amount) public payable{
        cAddress.call.value(amount)(bytes4(keccak256("newBAndTransfer()")));//2.1
        cAddress.call.value(amount + 1)(bytes4(keccak256("newBAndTransfer()")));//2.6
    }
}

contract B{
    constructor() payable public{}
    function() payable public{}
    function getOne() payable returns(uint256){
        return 1;
    }
    function callCGetZero(address cAddress,uint256 amount){
        cAddress.call.value(amount)(bytes4(keccak256("getZero()")));//1.5,1.7
    }
}

contract C{
    constructor() payable public{}
    function() payable public{}
    function getZero() payable public returns(uint256){
        return 0;
    }
    function newBAndTransfer() payable public returns(uint256){
        B b1 = (new B).value(7)();//2.2,2.7
        b1.getOne();//2.3,2.8
        B b2 = (new B).value(3)();//2.4,2.9
        b2.getOne();//2.5,2.10
    }
}