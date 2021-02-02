
contract VerifyBurnProof001Test {
    // verifyBurnProof(bytes32[10],bytes32[2],uint64,bytes32[2],bytes32)
    // size = 512
    //

    function VerifyBurnProofSize001(bytes32[10] memory output, bytes32[2] memory spendAuthoritySignature, uint64 value, bytes32[2] memory bindingSignature,bytes32 signHash) public returns (bool){
        return verifyBurnProof(output, spendAuthoritySignature, value, bindingSignature, signHash);
    }

    function VerifyBurnProofSize002(bytes memory data) public returns (bool, bytes memory){
        // bytes memory empty = "";
        return address(0x1000003).delegatecall(data);
    }

    function VerifyBurnProofSize003() public returns (bool, bytes memory){
        bytes memory empty = "";
        return address(0x1000003).delegatecall(empty);
    }
}