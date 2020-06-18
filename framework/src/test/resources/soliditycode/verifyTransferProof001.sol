contract verifyTransferProofTest {

    function test1() public returns (bool, bytes memory){
        bytes memory empty = "";
        return address(0x1000002).delegatecall(empty);
    }

    function test2(bytes memory data) public returns (bool, bytes memory){
        return address(0x1000002).delegatecall(data);
    }

    function test3(bytes32[10][] memory input, bytes32[2][] memory spendAuthoritySignature, bytes32[9][] memory output, bytes32[2] memory bindingSignature, bytes32 signHash, uint64 valueBalance, bytes32[33] memory frontier, uint256 leafCount) public returns (bytes32[] memory){
        return verifyTransferProof(input, spendAuthoritySignature, output, bindingSignature, signHash, valueBalance, frontier, leafCount);
    }
}