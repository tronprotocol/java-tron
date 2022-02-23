pragma solidity ^0.5.12;

contract IstanbulTest {
  constructor() public payable {}
  function getId() public view returns(uint256){
    uint256 id;
    assembly {
      id := chainid()
    }
    return id;
  }

  function getBalance(address src) public view returns(uint256){
    return address(src).balance;
  }

  function getBalance() public view returns(uint256){
    return address(this).balance;
  }
}