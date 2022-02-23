

contract A{
    constructor() payable public{}
    fallback() payable external{}
    function test1(address bAddr,address eAddr) public payable{
        bAddr.call.value(1)(abi.encodeWithSignature("testNN(address)",eAddr));//2.1
    }

}

contract B{
    constructor() payable public{}
    fallback() payable external{}
    function getOne() payable public returns(uint256){
        return 1;
    }
    function testNN(address eAddress) public payable{
         D d1=(new D).value(1000)();
         d1.getOne(eAddress);
    }
}

contract C{
    constructor() payable public{}
    fallback() payable external{}
    function getZero() payable public returns(uint256){
        return 0;
    }
    function newBAndTransfer() payable public returns(uint256){
        require(2==1);
    }
}
contract E{
    constructor() payable public{}
    fallback() payable external{}
    function getZero() payable public returns(uint256){
        return 0;
    }
    function newBAndTransfer() payable public returns(uint256){
        require(2==1);
    }
}
contract D{
    constructor() payable public{}
    fallback() payable external{}
    function getOne(address eAddress) payable public returns(uint256){
        eAddress.call.value(1)(abi.encodeWithSignature("getZero()"));//2.1
    }

}