//pragma solidity ^0.4.24;

contract A{
    constructor () payable public{}
    function selfdestruct(address toAddress) public payable{
        selfdestruct(toAddress);
    }
    function () payable external{}
    function getBalance() public view returns(uint256){
        return address(this).balance;
    }
}
contract B{
    function kill(address contractAddres, address toAddress) payable public {
        contractAddres.call(abi.encode(bytes4(keccak256("suicide(address)")),address(this)));
    }
    function kill2() public{
        A a = new A();
        a.selfdestruct(address(this));
    }
    function getBalance() public view returns(uint256){
        return address(this).balance;
    }
}