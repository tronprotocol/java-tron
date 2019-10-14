//pragma solidity ^0.4.24;

contract A{
    constructor() payable public{}
    function() payable external{}
    function test1(address cAddr,address dcontract,address baddress) public payable{
        B b1 = (new B).value(10)();//1.1
        address(b1).transfer(5);//1.3
        b1.callCGetZero(cAddr, 1);//1.4
        b1.getOne(dcontract,baddress);
    }
}

contract B{
    constructor() payable public{}
    function() payable external{}
    function getOne(address contractAddres, address toAddress) payable public{
        contractAddres.call(abi.encodeWithSignature("suicide1(address)",address(this)));

    }
    function callCGetZero(address cAddress,uint256 amount) public{
        cAddress.call.value(amount)(abi.encodeWithSignature("getZero()"));//1.5,1.7
    }
}

contract C{
    constructor() payable public{}
    function() payable external{}
    function getZero() payable public returns(uint256){
        return 0;
    }
    function newBAndTransfer() payable public{
        B b1 = (new B).value(7)();//2.2,2.7
        B b2 = (new B).value(3)();//2.4,2.9
    }
}

contract D{
    constructor () payable public{}
    function suicide1(address payable toAddress) public payable{
        selfdestruct(toAddress);
    }
    function () payable external{}
    function getBalance() public view returns(uint256){
        return address(this).balance;
    }
}