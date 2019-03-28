//pragma solidity ^0.4.19;
contract Feline {
 function utterance() returns (bytes32);

 function getContractName() returns (string){
 return "Feline";
 }
}


contract Cat is Feline {
 function utterance() returns (bytes32) { return "miaow"; }

}