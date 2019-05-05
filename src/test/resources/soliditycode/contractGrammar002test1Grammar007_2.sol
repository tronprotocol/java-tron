//pragma solidity ^0.4.19;
contract Doug{
 mapping (bytes32 => uint) public contracts;
 constructor() public{
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

 address public DOUG;

 address dogInterfaceAddress;
 DogInterface dogContract ;

 function setDOUG(address _doug) public  {
 DOUG = _doug;
 }

 constructor(address addr) public{
     dogInterfaceAddress = addr;
     dogContract = DogInterface(dogInterfaceAddress);
 }

 function dougOfage(uint _age) public  returns(uint) {

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