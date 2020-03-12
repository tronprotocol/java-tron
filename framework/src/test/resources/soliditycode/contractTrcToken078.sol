//pragma solidity ^0.4.24;
contract callerContract {
    constructor() public payable{}
    function() external payable{}
    function sendToB(address called_address, address c) public payable{
       called_address.delegatecall(abi.encodeWithSignature("transferTo(address)",c));
    }
    function sendToB2(address called_address,address c) public payable{
        called_address.call(abi.encodeWithSignature("transferTo(address)",c));
    }
    function sendToB3(address called_address,address c) public payable{
        called_address.delegatecall(abi.encodeWithSignature("transferTo(address)",c));
    }
}
   contract calledContract {
        function() external payable{}
       constructor() public payable {}
       function transferTo(address payable toAddress)public payable{
           toAddress.transfer(5);
       }

       function setIinC(address c) public payable{
           c.call.value(5)(abi.encode(bytes4(keccak256("setI()"))));
       }

   }
   contract c{
    address public origin;
    address public sender;
    constructor() public payable{}
    event log(address,address);
    function() payable external{
         emit log(tx.origin,msg.sender);
    }
   }