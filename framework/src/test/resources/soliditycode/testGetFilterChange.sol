pragma solidity ^0.8.0;
contract SolidityTest {
   event Deployed(address sender, uint256 a, uint256 num);
   function getResult(uint256 num) public payable  returns(uint256) {
      uint256 a=0;
      for(a=0;a<num;a++){
        emit Deployed(msg.sender, a , num);
      }
      return 1;
   }
}