

contract ConvertType {

constructor() payable public{}

fallback() payable external{}

//function trcTokenOnStorage(trcToken storage token) internal { // ERROR: Data location can only be specified for array, struct or mapping types, but "storage" was given.
//}

function trcTokenToString(trcToken token) public pure returns(string memory s){
// s = token; // ERROR
// s = string(token); // ERROR
}

function trcTokenToUint256(trcToken token) public pure returns(uint256 r){
uint256 u = token; // OK
uint256 u2 = uint256(token); // OK
r = u2;
}

function trcTokenToAddress(trcToken token) public pure returns(address r){
//r = token; // ERROR
token = 0x1234567812345678123456781234567812345678123456781234567812345678;
address a2 = address(token); // OK
r = a2;
}

function trcTokenToBytes(trcToken token) public pure returns(bytes memory r){
//r = token; // ERROR
// r = bytes(token); // ERROR
}

function trcTokenToBytes32(trcToken token) public pure returns(bytes32 r){
// r = token; // ERROR
bytes32 b2 = bytes32(token); // OK
r = b2;
}

function trcTokenToArray(trcToken token) public pure returns(uint[] memory r){
//r = token; // ERROR
}
}