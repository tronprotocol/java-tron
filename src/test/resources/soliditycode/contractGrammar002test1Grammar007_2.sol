//pragma solidity ^0.4.19;
contract Doug{
 mapping (bytes32 => uint) public contracts;
 constructor() public {
 contracts['hww'] = 1;
 contracts['brian'] = 2;
 contracts['zzy'] = 7;
 }

 function getDougName(string memory _name) public view returns(string memory) {
 return _name;
 }

 function getDougAge(uint _age) public pure returns(uint) {
 return 3 ** _age;
 }
}

contract DogInterface {
 function getDougAge(uint _age) public returns (uint);
 function contracts(bytes32 name) public returns (uint);
}
contract main{

 event FetchContract(address dogInterfaceAddress, address sender, bytes32 name);

 address DOUG;

 address payable dogInterfaceAddress = 0x7cDfa76B1C4566259734353C05af2Eac2959714A;
 DogInterface dogContract = DogInterface(dogInterfaceAddress);

 function setDOUG(address _doug) public {
 DOUG = _doug;
 }

 function dougOfage(uint _age) public payable returns(uint) {

 uint num = dogContract.getDougAge(_age);
 return _age+num;
 // return num;
 }

 function uintOfName(bytes32 _name) public returns (uint) {

 dogContract.contracts(_name);
 emit FetchContract(dogInterfaceAddress, msg.sender, _name);

 }

 // function getTest(string _name) public view returns(string) {
 // string memory newName = _name ;
 // DogInterface(DOUG).getDougName(newName);
 // return newName;
 // }
}