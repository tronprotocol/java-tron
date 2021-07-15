pragma experimental ABIEncoderV2;

// tests encoding from storage arrays

contract AbiEncode {
    int256[2][] tmp_h;
    function h(int256[2][] calldata s) external returns (bytes memory) {
        tmp_h = s;
        return abi.encode(tmp_h);
    }
    int256[2][2] tmp_i;
    function i(int256[2][2] calldata s) external returns (bytes memory) {
        tmp_i = s;
        return abi.encode(tmp_i);
    }
}