pragma experimental ABIEncoderV2;

contract Demo {
  bytes32 public result;

  constructor (bytes32 hash, bytes[] memory signatures, address[] memory addresses) public {
      result = multivalidatesign(hash, signatures, addresses);
  }

  function testConstructor() public returns(bytes32){
      return result;
  }

  function testConstructorPure() public view returns(bytes32){
      return result;
  }
}