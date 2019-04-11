pragma solidity ^0.4.24;

contract A{
    constructor() payable public{}
    function() payable public{}

    function test2(address cAddress,uint256 amount) public payable{
        cAddress.call.value(amount)();//2.1
    }
}


contract C{
    constructor() payable public{}
    function() payable public{}
    function getZero() payable public returns(uint256){
        return 0;
    }

}