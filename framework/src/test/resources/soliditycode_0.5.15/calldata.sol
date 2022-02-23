pragma experimental ABIEncoderV2;

contract C {
    struct S { uint256 a; }

    function f(S calldata s) external returns (bytes memory) {
        return abi.encode(s);
    }

    function g(S calldata s) external returns (bytes memory) {
        return this.f(s);
    }

    function m(uint256[] calldata) external pure returns (bytes memory) {
        return msg.data;
    }
    function h(uint8[] calldata s) external pure returns (bytes memory) {
        return abi.encode(s);
    }
    function i(uint8[][2] calldata s, uint256 which) external view returns (bytes memory) {
        return this.h(s[which]);
    }
    function j(bytes calldata s) external pure returns (bytes memory) {
        return abi.encode(s);
    }
    function k(bytes[2] calldata s, uint256 which) external view returns (bytes memory) {
        return this.j(s[which]);
    }
    function l(function() external returns (uint)[] calldata s) external returns (uint, uint, uint) {
        assert(s.length == 3);
        return (s[0](), s[1](), s[2]());
    }
}