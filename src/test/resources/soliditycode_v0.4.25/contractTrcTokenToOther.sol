//pragma solidity ^0.4.24;

contract ConvertType {

constructor() payable public{}

function() payable public{}

// function trcTokenOnStorage(trcToken storage token) internal { // ERROR Storage location can only be given for array or struct types
// }

function trcTokenToString(trcToken token) public constant returns(string r){
// string s = token; // ERROR
// string s2 = string(token); // ERROR
}

function trcTokenToUint256(trcToken token) public constant returns(uint256 r){
uint256 u = token; // OK
uint256 u2 = uint256(token); // OK
r = u2;
}

function trcTokenToAddress(trcToken token) public constant returns(address r){
// address a = token; // ERROR
token = 0x1234567812345678123456781234567812345678123456781234567812345678;
address a2 = address(token); // OK
r = a2;
}

function trcTokenToBytes(trcToken token) public constant returns(bytes r){
// bytes b = token; // ERROR
// bytes b2 = bytes(token); // ERROR
}

function trcTokenToBytes32(trcToken token) public constant returns(bytes32 r){
// bytes32 b = token; // ERROR
bytes32 b2 = bytes32(token); // OK
r = b2;
}

function trcTokenToArray(trcToken token) public constant returns(uint[] r){
// uint[] a = token; // ERROR
}
}