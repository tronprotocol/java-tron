//pragma solidity ^0.4.24;

contract callerContract {
    constructor() payable public{}
    function() payable external{}
    function sendToB(address called_address,address c) public payable{
       called_address.delegatecall(abi.encode(bytes4(keccak256("transferTo(address)")),c));
    }
    function sendToB2(address called_address,address c) public payable{
        called_address.call(abi.encode(bytes4(keccak256("transferTo(address)")),c));
    }
    function sendToB3(address called_address,address c) public payable{
        //called_address.callcode(bytes4(keccak256("transferTo(address)")),c);
        called_address.delegatecall(abi.encode(bytes4(keccak256("transferTo(address)")),c));
    }
}

   contract calledContract {
        function() payable external{}
       constructor() payable public{}
       function transferTo(address payable toAddress)public payable{
           toAddress.transfer(5);
       }

       function setIinC(address c) public payable{
           c.call.value(5)(abi.encode(bytes4(keccak256("setI()"))));
       }

   }

   contract c{
       uint256 public i=0;
       constructor() public payable{}
       function getBalance() public view returns(uint256){
           return address(this).balance;
       }
       function setI() payable public{
           i=5;
       }
       function() payable external{}
   }
