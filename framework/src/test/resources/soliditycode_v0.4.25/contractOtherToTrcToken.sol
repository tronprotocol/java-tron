pragma solidity ^0.4.24;
contract ConvertType {

constructor() payable public{}

function() payable external{}

//function stringToTrctoken(address payable toAddress, string memory tokenStr, uint256 tokenValue) public {
// trcToken t = trcToken(tokenStr); // ERROR
// toAddress.transferToken(tokenValue, tokenStr); // ERROR
//}

function uint256ToTrctoken(address toAddress,uint256 tokenValue, uint256 tokenInt)  public {
  trcToken t = trcToken(tokenInt); // OK
  toAddress.transferToken(tokenValue, t); // OK
  toAddress.transferToken(tokenValue, tokenInt); // OK
}

function addressToTrctoken(address toAddress, uint256 tokenValue, address adr) public {
  trcToken t = trcToken(adr); // OK
  toAddress.transferToken(tokenValue, t); // OK
//toAddress.transferToken(tokenValue, adr); // ERROR
}

//function bytesToTrctoken(address payable toAddress, bytes memory b, uint256 tokenValue) public {
 // trcToken t = trcToken(b); // ERROR
 // toAddress.transferToken(tokenValue, b); // ERROR
//}

function bytes32ToTrctoken(address toAddress, uint256 tokenValue, bytes32 b32) public {
  trcToken t = trcToken(b32); // OK
  toAddress.transferToken(tokenValue, t); // OK
// toAddress.transferToken(tokenValue, b32); // ERROR
}

//function arrayToTrctoken(address payable toAddress, uint256[] memory arr, uint256 tokenValue) public {
//trcToken t = trcToken(arr); // ERROR
// toAddress.transferToken(tokenValue, arr); // ERROR
//}
}