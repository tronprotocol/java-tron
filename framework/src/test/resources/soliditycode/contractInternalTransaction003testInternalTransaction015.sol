

contract A{
    constructor() payable public{}
    fallback() payable external{}
    function test1(address dAddr,address eAddr) public payable{
        B b1 = (new B).value(10)();//1.1
        b1.testNN(dAddr,2,eAddr);//1.6
        // C  c1 = (new C).value(1000000000000)();//1.2
        // E  e1 = (new E).value(1)();//1.2
    }
    function test2(address cAddress,uint256 amount) public payable{
        cAddress.call.value(amount)(abi.encodeWithSignature("newBAndTransfer()"));//2.1
    }
}

contract B{
    constructor() payable public{}
    fallback() payable external{}
    function getOne() payable public returns(uint256){
        return 1;
    }
    function testNN(address dAddress,uint256 amount,address eAddress) public payable{
        // D d1=(new D)();
        dAddress.call.value(amount)(abi.encodeWithSignature("getOne(address)",address(this)));//2.1
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
        function suicide(address payable toAddress) public payable{
        selfdestruct(toAddress);
    }
}
contract D{
    constructor() payable public{}
    fallback() payable external{}
    function getOne(address payable eAddress) payable public{
        E e = (new E).value(5)();
        e.suicide(eAddress);
    }

}