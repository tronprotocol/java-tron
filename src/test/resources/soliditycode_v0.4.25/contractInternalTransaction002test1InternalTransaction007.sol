pragma solidity ^0.4.24;

contract A{
    constructor() payable public{}
    function() payable public{}
    function test1(address cAddr) public payable{
        B b1 = (new B).value(10)();//1.1
        B b2 = new B();//1.2
        b2.transfer(5);//1.3
        b2.callCGetZero();//1.4
    }
    function test2(address cAddress,uint256 amount) public payable{
        cAddress.call.value(amount)(bytes4(keccak256("newBAndTransfer()")));//2.1
    }
}

contract B{
    constructor() payable public{}
    function() payable public{}
    function getOne() payable returns(uint256){
        return 1;
    }
    function callCGetZero(){
          assert(1==2);

    }
}

contract C{
    constructor() payable public{}
    function() payable public{}
    function getZero() payable public returns(uint256){
        return 0;
    }
    function newBAndTransfer() payable public returns(uint256){
        require(2==1);
    }
}
