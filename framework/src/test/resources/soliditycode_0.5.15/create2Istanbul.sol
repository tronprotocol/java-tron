pragma solidity ^0.5.12;

contract create2Istanbul {
  function deploy(bytes memory code, uint256 salt) public returns(address) {
    address addr;
    assembly {
      addr := create2(0, add(code, 0x20), mload(code), salt)
      if iszero(extcodesize(addr)) {
        revert(0, 0)
      }

    }
    return addr;
  }

  // prefix in main net is 0x41, testnet config is 0xa0
  function get(bytes1 prefix, bytes calldata code, uint256 salt) external view returns(address) {
    //bytes32 hash = keccak256(abi.encodePacked(bytes1(0x41),address(this), salt, keccak256(code)));
    bytes32 hash = keccak256(abi.encodePacked(prefix,address(this), salt, keccak256(code)));
    address addr = address(uint160(uint256(hash)));
    return addr;
  }

}

contract B {
  constructor() public payable{}
}