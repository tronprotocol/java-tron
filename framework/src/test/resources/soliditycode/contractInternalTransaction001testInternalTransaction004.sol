//pragma solidity ^0.4.24;

contract A{
    constructor () payable public{}
    function test(address payable  toAddress) public payable{
        selfdestruct(toAddress);
    }
    function () payable external{}
    function getBalance() public view returns(uint256){
        return address(this).balance;
    }
}
contract B{
    function() external payable{}
    function kill(address contractAddres, address toAddress) payable public {
        contractAddres.call(abi.encodeWithSignature("test(address)",address(this)));
    }
    function kill2() public{
        A a = new A();
        a.test(address(this));
    }
    function getBalance() public view returns(uint256){
        return address(this).balance;
    }
}
