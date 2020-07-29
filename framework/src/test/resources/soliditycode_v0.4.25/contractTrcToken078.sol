//pragma solidity ^0.4.24;
contract callerContract {
    constructor() payable{}
    function() payable{}
    function sendToB(address called_address,address c) public payable{
       called_address.delegatecall(bytes4(keccak256("transferTo(address)")),c);
    }
    function sendToB2(address called_address,address c) public payable{
        called_address.call(bytes4(keccak256("transferTo(address)")),c);
    }
    function sendToB3(address called_address,address c) public payable{
        called_address.callcode(bytes4(keccak256("transferTo(address)")),c);
    }
}
   contract calledContract {
        function() payable{}
       constructor() payable {}
       function transferTo(address toAddress)public payable{
           toAddress.transfer(5);
       }

       function setIinC(address c) public payable{
           c.call.value(5)(bytes4(keccak256("setI()")));
       }

   }
   contract c{
    address public origin;
    address public sender;
    constructor() public payable{}
    event log(address,address);
    function() payable public{
         emit log(tx.origin,msg.sender);
    }
   }