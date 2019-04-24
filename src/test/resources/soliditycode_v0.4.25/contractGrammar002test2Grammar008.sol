pragma solidity ^0.4.19;
contract Feline {
 function utterance() public returns (bytes32);

 function getContractName() public returns (string){
 return "Feline";
 }
}


contract Cat is Feline {
 function utterance() public returns (bytes32) { return "miaow"; }

}