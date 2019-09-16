pragma experimental ABIEncoderV2;

contract ecrecoverValidateSign {

    using ECVerify for bytes32;

    function validateSign(bytes32 hash,bytes[] memory sig,address[] memory signer) public returns (bool) {
        for(uint256 i=0;i<sig.length;i++){
            if (!hash.ecverify(sig[i], signer[i])) {
                return false;
            }
        }
        return true;
    }
}

library ECVerify {

    function recover(bytes32 hash, bytes memory signature) internal pure returns (address) {
        uint8 v;
        bytes32 r;
        bytes32 s;
        assembly {
            r := mload(add(signature, 32))
            s := mload(add(signature, 64))
            v := and(mload(add(signature, 65)), 255)
        }
        if(v<27){
            v+=27;
        }
        return ecrecover(hash, v, r, s);
    }

    function ecverify(bytes32 hash, bytes memory sig, address signer) internal pure returns (bool) {
        return signer == recover(hash, sig);
    }
}