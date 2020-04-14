pragma solidity ^0.5.8;
pragma experimental ABIEncoderV2;

contract TokenTRC20 {
    function transfer(address _to, uint256 _value) public returns (bool success);
    function transferFrom(address _from, address _to, uint256 _value) public returns (bool success);
}

contract PrivateUSDT {
    mapping(bytes32 => bytes32) public nullifiers; // store nullifiers of spent commitments
    mapping(bytes32 => bytes32) public roots; // store history root
    mapping(uint256 => bytes32) public tree;
    mapping(bytes32 => bytes32) public notecommitment;
    bytes32[32] zeroes = [bytes32(0x0100000000000000000000000000000000000000000000000000000000000000), bytes32(0x817de36ab2d57feb077634bca77819c8e0bd298c04f6fed0e6a83cc1356ca155), bytes32(0xffe9fc03f18b176c998806439ff0bb8ad193afdb27b2ccbc88856916dd804e34), bytes32(0xd8283386ef2ef07ebdbb4383c12a739a953a4d6e0d6fb1139a4036d693bfbb6c), bytes32(0xe110de65c907b9dea4ae0bd83a4b0a51bea175646a64c12b4c9f931b2cb31b49), bytes32(0x912d82b2c2bca231f71efcf61737fbf0a08befa0416215aeef53e8bb6d23390a), bytes32(0x8ac9cf9c391e3fd42891d27238a81a8a5c1d3a72b1bcbea8cf44a58ce7389613), bytes32(0xd6c639ac24b46bd19341c91b13fdcab31581ddaf7f1411336a271f3d0aa52813), bytes32(0x7b99abdc3730991cc9274727d7d82d28cb794edbc7034b4f0053ff7c4b680444), bytes32(0x43ff5457f13b926b61df552d4e402ee6dc1463f99a535f9a713439264d5b616b), bytes32(0xba49b659fbd0b7334211ea6a9d9df185c757e70aa81da562fb912b84f49bce72), bytes32(0x4777c8776a3b1e69b73a62fa701fa4f7a6282d9aee2c7a6b82e7937d7081c23c), bytes32(0xec677114c27206f5debc1c1ed66f95e2b1885da5b7be3d736b1de98579473048), bytes32(0x1b77dac4d24fb7258c3c528704c59430b630718bec486421837021cf75dab651), bytes32(0xbd74b25aacb92378a871bf27d225cfc26baca344a1ea35fdd94510f3d157082c), bytes32(0xd6acdedf95f608e09fa53fb43dcd0990475726c5131210c9e5caeab97f0e642f), bytes32(0x1ea6675f9551eeb9dfaaa9247bc9858270d3d3a4c5afa7177a984d5ed1be2451), bytes32(0x6edb16d01907b759977d7650dad7e3ec049af1a3d875380b697c862c9ec5d51c), bytes32(0xcd1c8dbf6e3acc7a80439bc4962cf25b9dce7c896f3a5bd70803fc5a0e33cf00), bytes32(0x6aca8448d8263e547d5ff2950e2ed3839e998d31cbc6ac9fd57bc6002b159216), bytes32(0x8d5fa43e5a10d11605ac7430ba1f5d81fb1b68d29a640405767749e841527673), bytes32(0x08eeab0c13abd6069e6310197bf80f9c1ea6de78fd19cbae24d4a520e6cf3023), bytes32(0x0769557bc682b1bf308646fd0b22e648e8b9e98f57e29f5af40f6edb833e2c49), bytes32(0x4c6937d78f42685f84b43ad3b7b00f81285662f85c6a68ef11d62ad1a3ee0850), bytes32(0xfee0e52802cb0c46b1eb4d376c62697f4759f6c8917fa352571202fd778fd712), bytes32(0x16d6252968971a83da8521d65382e61f0176646d771c91528e3276ee45383e4a), bytes32(0xd2e1642c9a462229289e5b0e3b7f9008e0301cbb93385ee0e21da2545073cb58), bytes32(0xa5122c08ff9c161d9ca6fc462073396c7d7d38e8ee48cdb3bea7e2230134ed6a), bytes32(0x28e7b841dcbc47cceb69d7cb8d94245fb7cb2ba3a7a6bc18f13f945f7dbd6e2a), bytes32(0xe1f34b034d4a3cd28557e2907ebf990c918f64ecb50a94f01d6fda5ca5c7ef72), bytes32(0x12935f14b676509b81eb49ef25f39269ed72309238b4c145803544b646dca62d), bytes32(0xb2eed031d4d6a4f02a097f80b54cc1541d4163c6b6f5971f88b6e41d35c53814)];
    bytes32[33] frontier;
    uint256 public leafCount;
    bytes32 public latestRoot;
    address _owner;
    TokenTRC20 _trc20Token;
 
    event newLeaf(uint256 position, bytes32 cm, bytes32 cv, bytes32 epk, bytes32[21] c);
    event tokenMint(address from, uint64 value);
    event tokenBurn(address to, uint64 value);
    //TODO  address
    constructor (address trc20ContractAddress) public {
        _owner = msg.sender;
        _trc20Token = TokenTRC20(trc20ContractAddress);
    }
    // output: cm, cv, epk, proof
    function mint(uint64 value, bytes32[9] calldata output, bytes32[2] calldata bindingSignature, bytes32[21] calldata c) external {
        address sender = msg.sender;
        require(value > 0, "Mint negative value.");
        require(notecommitment[output[0]] == 0, "Duplicate notecommitments");
        bytes32 signHash = sha256(abi.encodePacked(address(this), value, output, c));
        (bytes memory ret) = verifyMintProof(output, bindingSignature, value, signHash, frontier, leafCount);
        uint256 result = uint256(bytesToBytes32(ret, 0));
        require(result > 0, "The proof and signature have not been verified by the contract");
        uint256 slot = uint8(ret[32]);
        uint256 nodeIndex = leafCount + 2 ** 32 - 1;
        tree[nodeIndex] = output[0];
        if (slot == 0) {
            frontier[0] = output[0];
        }
        for (uint256 i = 1; i < slot + 1; i++) {
            nodeIndex = (nodeIndex - 1) / 2;
            tree[nodeIndex] = bytesToBytes32(ret, i * 32 + 1);
            if (i == slot) {
                frontier[slot] = tree[nodeIndex];
            }
        }
        latestRoot = bytesToBytes32(ret, slot * 32 + 33);
        roots[latestRoot] = latestRoot;
        leafCount ++;
        emit newLeaf(leafCount - 1, output[0], output[1], output[2], c);
        // Finally, transfer the trc20Token from the sender to this contract
        bool transferResult = _trc20Token.transferFrom(sender, address(this), value);
        require(transferResult, "TransferFrom failed.");
        emit tokenMint(sender, value);
    }
    //input_bytes32*10: nf, anchor, cv, rk, proof
    //output_bytes32*9: cm, cv, epk, proof
    function transfer(bytes32[10][] calldata input, bytes32[2][] calldata spend_auth_sig, bytes32[9][] calldata output, bytes32[2] calldata bindingSignature, bytes32[21][] calldata c) external {
        require(input.length >= 1 && input.length <= 2, "input number must be 1 or 2");
        require(input.length == spend_auth_sig.length, "input number must be equal to spend_auth_sig number");
        
        for (uint256 i = 0; i < input.length; i++) {
            require(nullifiers[input[i][0]] == 0, "The notecommitment being spent has already been nullified!");
            require(roots[input[i][1]] != 0, "The anchor must exist");
        }
        for (uint256 i = 0; i < output.length; i++) {
            require(notecommitment[output[i][0]] == 0, "Duplicate notecommitment");
        }
        bytes32 signHash = sha256(abi.encodePacked(address(this), input, output, c));
        require(output.length >= 1 && output.length <= 2, "output number must be 1 or 2");
        require(output.length == c.length, "output number must be equal to c number");
        (bytes memory ret) = verifyTransferProof(input, spend_auth_sig, output, bindingSignature, signHash, frontier, leafCount);
        uint256 result = uint256(bytesToBytes32(ret, 0));
        require(result > 0, "The proof and signature has not been verified by the contract");

        uint256 offset = 32;
        //ret offset
        for (uint256 i = 0; i < output.length; i++) {
            uint slot = uint8(ret[offset]);
            offset = offset + 1;
            uint256 nodeIndex = leafCount + 2 ** 32 - 1 + i;
            tree[nodeIndex] = output[i][0];
            //cm
            if (slot == 0) {
                frontier[0] = output[i][0];
            }
            for (uint256 k = 1; k < slot + 1; k++) {
                nodeIndex = (nodeIndex - 1) / 2;
                tree[nodeIndex] = bytesToBytes32(ret, offset);
                offset = offset + 32;
                if (k == slot) {
                    frontier[slot] = tree[nodeIndex];
                }
            }
            leafCount++;
        }
        latestRoot = bytesToBytes32(ret, offset);
        roots[latestRoot] = latestRoot;
        for (uint256 i = 0; i < input.length; i++) {
            bytes32 nf = input[i][0];
            nullifiers[nf] = nf;
        }
        for (uint256 i = 0; i < output.length; i++) {
            emit newLeaf(leafCount - (output.length - i), output[i][0], output[i][1], output[i][2], c[i]);
        }
    }
    //input_bytes32*10: nf, anchor, cv, rk, proof
    function burn(bytes32[10] calldata input, bytes32[2] calldata spend_auth_sig, uint64 value, bytes32[2] calldata bindingSignature, uint256 payToAddress) external {
        bytes32 nf = input[0];
        bytes32 anchor = input[1];
        require(value > 0, "Burn negative value.");
        require(nullifiers[nf] == 0, "The notecommitment being spent has already been nullified!");
        require(roots[anchor] != 0, "The anchor must exist");
        address payTo = address(payToAddress);
        bytes32 signHash = sha256(abi.encodePacked(address(this), input, payTo, value));
        (bool result) = verifyBurnProof(input, spend_auth_sig, value, bindingSignature, signHash);
        require(result, "The proof and signature have not been verified by the contract");
        nullifiers[nf] = nf;
        //Finally, transfer trc20Token from this contract to the nominated address
        bool transferResult = _trc20Token.transfer(payTo, value);
        require(transferResult, "transfer failed.");
        emit tokenBurn(payTo, value);
    }
    //position: index of leafnode, start from 0
    function getPath(uint256 position) public view returns (bytes32, bytes32[32] memory, uint256){
        uint256 index = position + 2 ** 32 - 1;
        bytes32[32] memory path;
        require(position >= 0, "position should be non-negative");
        require(position < leafCount, "position should be smaller than leafCount");
        uint32 level = ancestorLevel(position);
        bytes32 targetNodeValue = getTargetNodeValue(position, level);
        for (uint32 i = 0; i < 32; i++) {
            if (i == level) {
                path[31 - i] = targetNodeValue;
            } else {
                if (index % 2 == 0) {
                    path[31 - i] = tree[index - 1];
                } else {
                    path[31 - i] = tree[index + 1] == 0 ? zeroes[i] : tree[index + 1];
                }
            }
            index = (index - 1) / 2;
        }
        return (latestRoot, path, position);
    }
    function ancestorLevel(uint256 leafIndex) private view returns (uint32) {
        uint256 nodeIndex1 = leafIndex + 2 ** 32 - 1;
        uint256 nodeIndex2 = leafCount + 2 ** 32 - 2;
        uint32 level = 0;
        while (((nodeIndex1 - 1) / 2) != ((nodeIndex2 - 1) / 2)) {
            nodeIndex1 = (nodeIndex1 - 1) / 2;
            nodeIndex2 = (nodeIndex2 - 1) / 2;
            level = level + 1;
        }
        return level;
    }
    function getTargetNodeValue(uint256 leafIndex, uint32 level) private view returns (bytes32) {
        bytes32 left;
        bytes32 right;
        uint256 index = leafIndex + 2 ** 32 - 1;
        uint256 nodeIndex = leafCount + 2 ** 32 - 2;
        bytes32 nodeValue = tree[nodeIndex];
        if (level == 0) {
            if (index < nodeIndex) {
                return nodeValue;
            }
            if (index == nodeIndex) {
                if (index % 2 == 0) {
                    return tree[index - 1];
                } else {
                    return zeroes[0];
                }
            }
        }
        for (uint32 i = 0; i < level; i++) {
            if (nodeIndex % 2 == 0) {
                left = tree[nodeIndex - 1];
                right = nodeValue;
            } else {
                left = nodeValue;
                right = zeroes[i];
            }
            nodeValue = pedersenHash(i, left, right);
            nodeIndex = (nodeIndex - 1) / 2;
        }
        return nodeValue;
    }

    function bytesToBytes32(bytes memory b, uint offset) private returns (bytes32) {
        bytes32 out;
        for (uint i = 0; i < 32; i++) {
            out |= bytes32(b[i + offset] & 0xFF) >> (i * 8);
        }
        return out;
    }
}