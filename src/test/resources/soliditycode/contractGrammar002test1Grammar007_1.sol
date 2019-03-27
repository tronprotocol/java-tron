pragma solidity ^0.4.19;
contract Doug{
 mapping (bytes32 => uint) public contracts;
 function Doug() {
 contracts['hww'] = 1;
 contracts['brian'] = 2;
 contracts['zzy'] = 7;
 }
 
 function getDougName(string _name) public view returns(string) {
 return _name;
 }
 
 function getDougAge(uint _age) public pure returns(uint) {
 return 3 ** _age;
 }
}