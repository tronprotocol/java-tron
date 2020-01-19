pragma solidity ^0.5.8;
pragma experimental ABIEncoderV2;

//import "./MerkleTree.sol";

contract PrivateUSDT {

    //USDTToken private usdtToken; // the  ERC-20 token contract

    mapping(bytes32 => bytes32) public nullifiers; // store nullifiers of spent commitments
    mapping(bytes32 => bytes32) public roots; // holds each root we've calculated so that we can pull the one relevant to the prover
    bytes32 public latestRoot; // holds the index for the latest root so that the prover can provide it later and this contract can look up the relevant root
    address _owner;

    bytes32[33] frontier;
    uint256 public leafCount;

    constructor () public {
        _owner = msg.sender;
        //usdtToken = USDTToken(_USDToken);
    }


    address verifyProofContract = address(0x000F);
    // input: cv, epk, proof, bindingSig,
    function mint(uint64 value, bytes32 note_commitment, bytes32[10] calldata input, bytes32 signHash) external {
        require(value > 0, "Mint negative value.");
        //bytes32 signHash = keccak256(abi.encode(address(this), msg.sender, value, outputDescription));

        //1504 = 288 + 64 + "32" + 32 + 32*33 + 32
        (bool result,bytes memory msg) = verifyProofContract.call(abi.encode(note_commitment, input, value, signHash, frontier, leafCount));
        require(result, "The proof and signature have not been verified by the contract");

        uint slot = uint8(msg[64]);
        frontier[slot] = bytesToBytes32(msg, 0);
        latestRoot = bytesToBytes32(msg, 32);

        roots[latestRoot] = latestRoot;
        leafCount ++;
        // Finally, transfer the fTokens from the sender to this contract
        //usdtToken.transferFrom(msg.sender, address(this), value);
    }


    //input_bytes32*10: cv, rk, spend_auth_sig, proof
    //output1_bytes32*9: cv, cm, epk, proof
    //output2_bytes32*9: cv, cm, epk, proof
    function transfer(bytes32[10] calldata input, bytes32 anchor, bytes32 nf, bytes32[9] calldata output1, bytes32[9] calldata output2, bytes32[2] calldata bindingSignature, bytes32 signHash) external {

        require(nullifiers[nf] == 0, "The notecommitment being spent has already been nullified!");
        require(roots[anchor] != 0, "The anchor must exist");
        //bytes32 signHash = keccak256(abi.encode(address(this), msg.sender, spendDescription, outputDescription));

        //2144 = 384 + 288 + 288 + 64 + 32 + 32*33 + 32
        (bool result,bytes memory msg) = verifyProofContract.call(abi.encode(input, anchor, nf, output1, output2, bindingSignature, signHash, frontier, leafCount));
        require(result, "The proof and signature has not been verified by the contract");

        uint slot1 = uint8(msg[96]);
        uint slot2 = uint8(msg[97]);
        frontier[slot1] = bytesToBytes32(msg, 0);
        frontier[slot2] = bytesToBytes32(msg, 32);
        latestRoot = bytesToBytes32(msg, 64);

        roots[latestRoot] = latestRoot;
        leafCount = leafCount + 2;

        nullifiers[nf] = nf;

    }

    //input_bytes32*10: cv, rk, spend_auth_sig, proof
    function burn(bytes32[10] calldata input, bytes32 anchor, bytes32 nf, uint64 value, bytes32[2] calldata bindingSignature, bytes32 signHash) external {

        require(value > 0, "Mint negative value.");
        require(nullifiers[nf] == 0, "The notecommitment being spent has already been nullified!");
        require(roots[anchor] != 0, "The anchor must exist");

        //bytes32 signHash = keccak256(abi.encode(address(this), msg.sender, spendDescription, payTo, value));
        // 512 = 384 + 64 + 32 + 32
        (bool result,bytes memory msg) = verifyProofContract.call(abi.encode(input, anchor, nl;sf, value, bindingSignature, signHash));
        require(result, "The proof and signature have not been verified by the contract");

        nullifiers[nf] = nf;
        //Finally, transfer USDT from this contract to the nominated address
        //address payToAddress = address(payTo);
        //usdtToken.transfer(payToAddress, value);
    }
    function bytesToBytes32(bytes memory b, uint offset) private returns (bytes32) {
        bytes32 out;

        for (uint i = 0; i < 32; i++) {
            out |= bytes32(b[i+offset] & 0xFF) >> (i * 8);
        }
        return out;
    }

}
