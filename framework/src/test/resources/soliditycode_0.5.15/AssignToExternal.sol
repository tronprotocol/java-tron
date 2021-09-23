contract AssignToExternal {
    //    Not allow:
    //    function f(uint256[] calldata x, uint256[] calldata y) external pure {
    //        x = y;
    //    }

    //  allow:

    function f(uint256 a) external returns (uint){
        a = a + 1;
        return a;
    }

    function StringSet(string calldata a) external returns (string memory){
        return a;
    }

    function ByteSet(bytes32 a) external returns (bytes32){
        return a;
    }

    function UintArraySet(uint256[2] calldata a) external returns (uint256[2] memory){
        return a;
    }

    function AddSet(address a) external returns (address){
        return a;
    }

}
