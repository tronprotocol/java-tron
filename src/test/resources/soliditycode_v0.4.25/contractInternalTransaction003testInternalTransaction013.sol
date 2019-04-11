pragma solidity ^0.4.24;

contract A{
    constructor() payable public{}
    function() payable public{}
    function test1(address dAddr) public payable{
        B b1 = (new B).value(10)();//1.1
        b1.testNN(dAddr,2);//1.6
        // C  c1 = (new C).value(1000000000000)();//1.2
        // E  e1 = (new E).value(1)();//1.2
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
    function testNN(address dAddress,uint256 amount) public payable{
        // D d1=(new D)();
        dAddress.call.value(amount)(bytes4(keccak256("getOne()")));//2.1
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
contract E{
    constructor() payable public{}
    function() payable public{}
    function getZero() payable public returns(uint256){
        return 0;
    }
    function newBAndTransfer() payable public returns(uint256){
        require(2==1);
    }
}
contract D{
    constructor() payable public{}
    function() payable public{}
    function getOne() payable returns(uint256){
        E e = (new E).value(5)();
    }

}