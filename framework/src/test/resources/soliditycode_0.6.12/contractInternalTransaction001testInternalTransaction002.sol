
contract A{
    constructor() payable public{}
    fallback() payable external{}

    function test2(address cAddress,uint256 amount) public payable{
        //cAddress.call.value(amount)();//2.1
        cAddress.call.value(amount)("");//2.1
    }
}


contract C{
    constructor() payable public{}
    fallback() payable external{}
    function getZero() payable public returns(uint256){
        return 0;
    }

}