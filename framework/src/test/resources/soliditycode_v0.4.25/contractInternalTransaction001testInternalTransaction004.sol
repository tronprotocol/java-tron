pragma solidity ^0.4.24;

contract A{
    constructor () payable public{}
    function suicide(address toAddress) public payable{
        selfdestruct(toAddress);
    }
    function () payable public{}
    function getBalance() public view returns(uint256){
        return this.balance;
    }
}
contract B{
    function kill(address contractAddres, address toAddress) payable public {
        contractAddres.call(bytes4(keccak256("suicide(address)")),address(this));
    }
    function kill2(){
        A a = new A();
        a.suicide(this);
    }
    function getBalance() public view returns(uint256){
        return this.balance;
    }
}