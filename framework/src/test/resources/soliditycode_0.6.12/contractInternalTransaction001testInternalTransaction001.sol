
contract A{
    constructor() payable public{}
    fallback() payable external{}
    function test1(address payable cAddr) public payable{
        B b1 = (new B).value(10)();//1.1
        B b2 = new B();//1.2
        payable(address(b2)).transfer(5);//1.3
        b2.callCGetZero(cAddr, 1);//1.4
        b2.callCGetZero(cAddr,2);//1.6
    }
    function test2(address payable cAddress,uint256 amount) public payable{
        cAddress.call.value(amount)(abi.encodeWithSignature("newBAndTransfer()"));//2.1
        cAddress.call.value(amount + 1)(abi.encodeWithSignature("newBAndTransfer()"));//2.6
    }
}

contract B{
    constructor() payable public{}
    fallback() payable external{}
    function getOne() payable  public returns(uint256){
        return 1;
    }
    function callCGetZero(address payable cAddress,uint256 amount) public{
        cAddress.call.value(amount)(abi.encodeWithSignature("getZero()"));//1.5,1.7
    }
}

contract C{
    constructor() payable public{}
    fallback() payable external{}
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