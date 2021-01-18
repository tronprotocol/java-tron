
contract VerifyMintProof001Test {
    // verifyMintProof(bytes32[9],bytes32[2],uint64,bytes32,bytes32[33],uint256)

    function VerifyMintProofSize001(bytes32[9] memory output, bytes32[2] memory bindingSignature, uint64 value, bytes32 signHash, bytes32[33] memory frontier,uint256 leafCount) public returns (bytes32[] memory){
        return verifyMintProof(output, bindingSignature, value, signHash, frontier, leafCount);
    }

    function VerifyMintProofSize002(bytes memory data) public returns (bool, bytes memory){
//        address verifyMint = address (0x1000001);
//
//        assembly {
//            let succeeded := delegatecall(sub(gas, 5000), verifyMint, add(data, 0x20), mload(data), 0, 0)
//            let size := returndatasize
//            let response := mload(0x40)
//            mstore(0x40, add(response, and(add(add(size, 0x20), 0x1f), not(0x1f))))
//            mstore(response, size)
//            returndatacopy(add(response, 0x20), 0, size)
//            switch iszero(succeeded)
//            case 1 {
//            // throw if delegatecall failed
//                revert(add(response, 0x20), size)
//            }
//        }

        return address(0x1000001).delegatecall(data);
    }

    function VerifyMintProofSize003() public returns (bool, bytes memory){
        bytes memory empty = "";
        return address(0x1000001).call(empty);
    }
}