pragma solidity ^0.4.19;

contract main{

 event FetchContract(address dogInterfaceAddress, address sender, bytes32 name);

 address DOUG;

 address dogInterfaceAddress = 0x4c1c6fe3043368095a0aae8123b83bdbfee653f0;
 DogInterface dogContract = DogInterface(dogInterfaceAddress);

 function setDOUG(address _doug) {
 DOUG = _doug;
 }

 function dougOfage(uint _age) public view returns(uint) {

 uint num = dogContract.getDougAge(_age);
 return _age+num;
 // return num;
 }

 function uintOfName(bytes32 _name) returns (uint) {

 dogContract.contracts(_name);
 FetchContract(dogInterfaceAddress, msg.sender, _name);

 }

 // function getTest(string _name) public view returns(string) {
 // string memory newName = _name ;
 // DogInterface(DOUG).getDougName(newName);
 // return newName;
 // }


}