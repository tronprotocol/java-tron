//pragma solidity ^0.4.24;

contract ConvertType {

constructor() payable public{}

function() payable public{}

function stringToTrctoken(address toAddress, string tokenStr, uint256 tokenValue) public {
// trcToken t = trcToken(tokenStr); // ERROR
// toAddress.transferToken(tokenValue, tokenStr); // ERROR
}

function uint256ToTrctoken(address toAddress, uint256 tokenInt, uint256 tokenValue) public {
trcToken t = trcToken(tokenInt); // OK
toAddress.transferToken(tokenValue, t); // OK
toAddress.transferToken(tokenValue, tokenInt); // OK
}

function addressToTrctoken(address toAddress, address adr, uint256 tokenValue) public {
trcToken t = trcToken(adr); // OK
toAddress.transferToken(tokenValue, t); // OK
// toAddress.transferToken(tokenValue, adr); // ERROR
}

function bytesToTrctoken(address toAddress, bytes b, uint256 tokenValue) public {
// trcToken t = trcToken(b); // ERROR
// toAddress.transferToken(tokenValue, b); // ERROR
}

function bytes32ToTrctoken(address toAddress, bytes32 b32, uint256 tokenValue) public {
trcToken t = trcToken(b32); // OK
toAddress.transferToken(tokenValue, t); // OK
// toAddress.transferToken(tokenValue, b32); // ERROR
}

function arrayToTrctoken(address toAddress, uint256[] arr, uint256 tokenValue) public {
trcToken t = trcToken(arr); // ERROR
toAddress.transferToken(tokenValue, arr); // ERROR
}
}