contract AltBn128 {
  constructor() public payable {}
  function callBn256Add(bytes32 ax, bytes32 ay, bytes32 bx, bytes32 by) public returns (bytes32[2] memory result) {
    bytes32[4] memory input;
    input[0] = ax;
    input[1] = ay;
    input[2] = bx;
    input[3] = by;
    assembly {
      let success := call(gas(), 0x06, 0, input, 0x80, result, 0x40)
    }

  }

  function callBn256AddNoValue(bytes32 ax, bytes32 ay, bytes32 bx, bytes32 by) public returns
  (bytes32[2] memory result) {
    bytes32[4] memory input;
    input[0] = ax;
    input[1] = ay;
    input[2] = bx;
    input[3] = by;
    assembly {
      let success := call(gas(), 0xac, 0, input, 0x80, result, 0x40)
    }
  }

  function callBn256ScalarMul(bytes32 x, bytes32 y, bytes32 scalar) public returns (bytes32[2] memory result) {
    bytes32[3] memory input;
    input[0] = x;
    input[1] = y;
    input[2] = scalar;
    assembly {
      let success := call(gas(), 0x07, 0, input, 0x60, result, 0x40)
      switch success
      case 0 {
        revert(0,0)
      }
    }
  }

  function callBn256Pairing(bytes memory input) public returns (bytes32 result) {
    // input is a serialized bytes stream of (a1, b1, a2, b2, ..., ak, bk) from (G_1 x G_2)^k
    uint256 len = input.length;
    require(len % 192 == 0);
    assembly {
      let memPtr := mload(0x40)
      let success := call(gas(), 0x08, 0, add(input, 0x20), len, memPtr, 0x20)
      switch success
      case 0 {
        revert(0,0)
      } default {
        result := mload(memPtr)
      }
    }
  }

  function convert(uint256 num) public view returns(bytes32) {
    return bytes32(num);
  }

}