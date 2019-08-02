//pragma solidity ^0.4.24;

 contract tokenTest{
     constructor() public payable{}
     // positive case
     function TransferTokenTo(address toAddress, trcToken id,uint256 amount) public payable{
         //trcToken id = 0x74657374546f6b656e;
         toAddress.transferToken(amount,id);
     }
 }