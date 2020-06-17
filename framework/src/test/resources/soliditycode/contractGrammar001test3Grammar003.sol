//pragma solidity ^0.4.11;

library Set {
 struct Data { mapping(uint => bool) flags; }

 function insert(Data storage self, uint value) public
 returns (bool)
 {
 if (self.flags[value])
 return false; // already there
 self.flags[value] = true;
 return true;
 }

 function remove(Data storage self, uint value) public
 returns (bool)
 {
 if (!self.flags[value])
 return false; // not there
 self.flags[value] = false;
 return true;
 }

 function contains(Data storage self, uint value) public
 returns (bool)
 {
 return self.flags[value];
 }
}


contract C {
 using Set for Set.Data; // this is the crucial change
 Set.Data knownValues;

 function register(uint value) public{
 // Here, all variables of type Set.Data have
 // corresponding member functions.
 // The following function call is identical to
 // Set.insert(knownValues, value)
 if (!knownValues.insert(value))
 revert();
 }
}