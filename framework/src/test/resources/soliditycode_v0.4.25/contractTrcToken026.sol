pragma solidity ^0.4.24;

contract token{
    constructor() payable public{}
    function() payable public{}
     function testInCall(address callBAddress,address callCAddress, address toAddress ,uint256 amount,trcToken id) payable public{
         callBAddress.call(bytes4(keccak256("transC(address,address,uint256,trcToken)")),callCAddress,toAddress,amount,id);
     }
    function testIndelegateCall(address callBddress,address callAddressC, address toAddress,uint256 amount, trcToken id) payable public{
         callBddress.delegatecall(bytes4(keccak256("transC(address,address,uint256,trcToken)")),callAddressC,toAddress,amount,id);
     }
 }



contract B{
    constructor() public payable{}
    function() public payable{}
    function  transC(address callCAddress,address toAddress,uint256 amount, trcToken id) payable public{
         callCAddress.call(bytes4(keccak256("trans(address,uint256,trcToken)")),toAddress,amount,id);
    }
}
contract C{
    constructor() payable public{}
    function() payable public{}
    function  trans(address toAddress,uint256 amount, trcToken id) payable public{
            toAddress.transferToken(amount,id);
    }

}
