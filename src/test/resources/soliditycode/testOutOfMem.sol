contract Test {
    function testOutOfMem(uint256 x) public returns(bytes32 r) {
        uint[] memory memVar;
        memVar = new uint[](x);
    }

}