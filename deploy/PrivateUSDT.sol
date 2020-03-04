pragma solidity ^0.5.8;
pragma experimental ABIEncoderV2;

contract PrivateUSDT {

    //USDTToken private usdtToken; // the  ERC-20 token contract

    mapping(bytes32 => bytes32) public nullifiers; // store nullifiers of spent commitments
    mapping(bytes32 => bytes32) public roots; // store history root
    bytes32 public latestRoot;
    address _owner;

    mapping(uint256 => bytes32) public tree;
    bytes32[32] zeroes = [bytes32(0x0100000000000000000000000000000000000000000000000000000000000000),bytes32(0x817de36ab2d57feb077634bca77819c8e0bd298c04f6fed0e6a83cc1356ca155),bytes32(0xffe9fc03f18b176c998806439ff0bb8ad193afdb27b2ccbc88856916dd804e34),bytes32(0xd8283386ef2ef07ebdbb4383c12a739a953a4d6e0d6fb1139a4036d693bfbb6c),bytes32(0xe110de65c907b9dea4ae0bd83a4b0a51bea175646a64c12b4c9f931b2cb31b49),bytes32(0x912d82b2c2bca231f71efcf61737fbf0a08befa0416215aeef53e8bb6d23390a),bytes32(0x8ac9cf9c391e3fd42891d27238a81a8a5c1d3a72b1bcbea8cf44a58ce7389613),bytes32(0xd6c639ac24b46bd19341c91b13fdcab31581ddaf7f1411336a271f3d0aa52813),bytes32(0x7b99abdc3730991cc9274727d7d82d28cb794edbc7034b4f0053ff7c4b680444),bytes32(0x43ff5457f13b926b61df552d4e402ee6dc1463f99a535f9a713439264d5b616b),bytes32(0xba49b659fbd0b7334211ea6a9d9df185c757e70aa81da562fb912b84f49bce72),bytes32(0x4777c8776a3b1e69b73a62fa701fa4f7a6282d9aee2c7a6b82e7937d7081c23c),bytes32(0xec677114c27206f5debc1c1ed66f95e2b1885da5b7be3d736b1de98579473048),bytes32(0x1b77dac4d24fb7258c3c528704c59430b630718bec486421837021cf75dab651),bytes32(0xbd74b25aacb92378a871bf27d225cfc26baca344a1ea35fdd94510f3d157082c),bytes32(0xd6acdedf95f608e09fa53fb43dcd0990475726c5131210c9e5caeab97f0e642f),bytes32(0x1ea6675f9551eeb9dfaaa9247bc9858270d3d3a4c5afa7177a984d5ed1be2451),bytes32(0x6edb16d01907b759977d7650dad7e3ec049af1a3d875380b697c862c9ec5d51c),bytes32(0xcd1c8dbf6e3acc7a80439bc4962cf25b9dce7c896f3a5bd70803fc5a0e33cf00),bytes32(0x6aca8448d8263e547d5ff2950e2ed3839e998d31cbc6ac9fd57bc6002b159216),bytes32(0x8d5fa43e5a10d11605ac7430ba1f5d81fb1b68d29a640405767749e841527673),bytes32(0x08eeab0c13abd6069e6310197bf80f9c1ea6de78fd19cbae24d4a520e6cf3023),bytes32(0x0769557bc682b1bf308646fd0b22e648e8b9e98f57e29f5af40f6edb833e2c49),bytes32(0x4c6937d78f42685f84b43ad3b7b00f81285662f85c6a68ef11d62ad1a3ee0850),bytes32(0xfee0e52802cb0c46b1eb4d376c62697f4759f6c8917fa352571202fd778fd712),bytes32(0x16d6252968971a83da8521d65382e61f0176646d771c91528e3276ee45383e4a),bytes32(0xd2e1642c9a462229289e5b0e3b7f9008e0301cbb93385ee0e21da2545073cb58),bytes32(0xa5122c08ff9c161d9ca6fc462073396c7d7d38e8ee48cdb3bea7e2230134ed6a),bytes32(0x28e7b841dcbc47cceb69d7cb8d94245fb7cb2ba3a7a6bc18f13f945f7dbd6e2a),bytes32(0xe1f34b034d4a3cd28557e2907ebf990c918f64ecb50a94f01d6fda5ca5c7ef72),bytes32(0x12935f14b676509b81eb49ef25f39269ed72309238b4c145803544b646dca62d),bytes32(0xb2eed031d4d6a4f02a097f80b54cc1541d4163c6b6f5971f88b6e41d35c53814)];
    bytes32[33] frontier;
    uint256 public leafCount;

    address verifyProofContract = address(0x000F);
    address hashor = address(0x0010);
    address calTimeContract = address(0x0011);

    //event returnPath(uint256 index, bytes32[33] path);

    event returnCurrentTime(bytes time);

    constructor () public {
        _owner = msg.sender;
        //usdtToken = USDTToken(_USDToken);
    }

    function ancestorLevel(uint256 leafIndex) private returns(uint32) {
        uint256 nodeIndex1 =  leafIndex + 2 ** 32 - 1;
        uint256 nodeIndex2 = leafCount + 2 ** 32 - 2;
        uint32 level = 0;
        while (((nodeIndex1 - 1) / 2) != ((nodeIndex2 -1) / 2)) {
            nodeIndex1 = (nodeIndex1 - 1) / 2;
            nodeIndex2 = (nodeIndex2 - 1) / 2;
            level = level + 1;
        }

        return level;
    }

    function getTargetNodeValue(uint256 leafIndex, uint32 level) private returns(bytes32) {
        bytes32 left;
        bytes32 right;
        uint256 index = leafIndex + 2 ** 32 - 1;
        uint256 nodeIndex = leafCount + 2 ** 32 - 2;
        bytes32 nodeValue = tree[nodeIndex];
        if(level == 0) {
            if(index < nodeIndex) {
                return nodeValue;
            }
            if(index == nodeIndex) {
                if(index % 2 == 0){
                    return tree[index - 1];
                } else {
                    return zeroes[0];
                }
            }
        }

        for(uint32 i = 0; i < level; i++) {
            if (nodeIndex % 2 == 0) {
                left  = tree[nodeIndex - 1];
                right = nodeValue;
            } else {
                left = nodeValue;
                right = zeroes[level];
            }

            (bool result,bytes memory msg) = hashor.call(abi.encode(i, left, right));
            require(result, "hash error");

            nodeValue = bytesToBytes32(msg, 0);
            nodeIndex = (nodeIndex - 1) / 2;
        }

        return nodeValue;
    }
    //position: index of leafnode, start from 0
    function getPath(uint256 position) external returns(bytes32, bytes32[32] memory, uint256){
        uint256 index = position + 2**32 - 1;
        bytes32[32] memory path;
        require(position >= 0, "position should be non-negative");
        require(position < leafCount, "position should be smaller than leafCount");

        uint32 level = ancestorLevel(position);
        bytes32 targetNodeValue = getTargetNodeValue(position, level);

        for (uint32 i = 0; i < 32; i++){
            if(i == level) {
                path[31-i] = targetNodeValue;
            } else {
                if (index % 2 == 0) {
                    path[31-i] = tree[index - 1];
                } else {
                    path[31-i] = tree[index + 1] == 0?zeroes[i]:tree[index + 1];
                }
            }
            index = (index - 1) / 2;
        }
        return (latestRoot, path, position);
    }

    // get current time
    function getCurrentTime() private returns (bytes memory) {
        (bool result, bytes memory msg) = calTimeContract.call("");
        require(result, "getCurrentTime failed");

//        uint64 time = bytesToUint64(msg);

        return msg;
    }

    // print delta and get new current time
    function calDeltaTime(bytes memory param) private returns (bytes memory) {
        (bool result, bytes memory msg) = calTimeContract.call(abi.encode(param));
        require(result, "calDeltaTime failed");

//        uint64 time = bytesToUint64(msg);

        return msg;
    }

    // input: cv, epk, proof, bindingSig
    function mint(uint64 value, bytes32 note_commitment, bytes32[10] calldata input, bytes32 signHash) external {
        require(value > 0, "Mint negative value.");
        //bytes32 signHash = keccak256(abi.encode(address(this), msg.sender, value, outputDescription));

        //1504 = 288 + 64 + "32" + 32 + 32*33 + 32
        (bool result,bytes memory msg) = verifyProofContract.call(abi.encode(note_commitment, input, value, signHash, frontier, leafCount));
        require(result, "The proof and signature have not been verified by the contract");

        // get time
        (bool r1, bytes memory t1) = calTimeContract.call("");
        require(r1, "getCurrentTime failed");
        bytes8 startTime = bytesToBytes8(t1, 0);

        uint256 slot = uint8(msg[0]);
        uint256 nodeIndex = leafCount + 2 ** 32 - 1;
        tree[nodeIndex] = note_commitment;
        if(slot == 0){
            frontier[0] = note_commitment;
        }
        for (uint256 i = 1; i < slot+1; i++) {
            nodeIndex = (nodeIndex - 1) / 2;
            tree[nodeIndex] = bytesToBytes32(msg, i*32-31);
            if(i == slot){
                frontier[slot] = tree[nodeIndex];
            }
        }
        latestRoot = bytesToBytes32(msg, slot*32+1);
        tree[0]= latestRoot;

        roots[latestRoot] = latestRoot;
        leafCount ++;
        // Finally, transfer the fTokens from the sender to this contract
        //usdtToken.transferFrom(msg.sender, address(this), value);

        // cal delta time, slot process
        (bool r2, bytes memory t2) = calTimeContract.call(abi.encode(startTime));
        require(r2, "getCurrentTime failed");
//        bytes8 startTimeSlot = bytesToBytes8(t2, 0);
    }


    //input_bytes32*10: cv, rk, spend_auth_sig, proof
    //output1_bytes32*9: cv, cm, epk, proof
    //output2_bytes32*9: cv, cm, epk, proof
    function transfer(bytes32[10] calldata input, bytes32 anchor, bytes32 nf, bytes32[9] calldata output1, bytes32[9] calldata output2, bytes32[2] calldata bindingSignature, bytes32 signHash) external {

        // require(nullifiers[nf] == 0, "The notecommitment being spent has already been nullified!");
        require(roots[anchor] != 0, "The anchor must exist");
        //bytes32 signHash = keccak256(abi.encode(address(this), msg.sender, spendDescription, outputDescription));

        //2144 = 384 + 288 + 288 + 64 + 32 + 32*33 + 32
        (bool result,bytes memory msg) = verifyProofContract.call(abi.encode(input, anchor, nf, output1, output2, bindingSignature, signHash, frontier, leafCount));
        require(result, "The proof and signature has not been verified by the contract");

        // get time
        (bool r1, bytes memory t1) = calTimeContract.call("");
        require(r1, "getCurrentTime failed");
        bytes8 startTime = bytesToBytes8(t1, 0);

        uint slot1 = uint8(msg[0]);
        uint slot2 = uint8(msg[1]);
        //process slot1
        uint256 nodeIndex = leafCount + 2 ** 32 - 1;
        tree[nodeIndex] = output1[1];//cm
        if(slot1 == 0){
            frontier[0] = output1[1];
        }
        for (uint256 i = 1; i < slot1+1; i++) {
            nodeIndex = (nodeIndex - 1) / 2;
            tree[nodeIndex] = bytesToBytes32(msg, i * 32 - 30);
            if(i == slot1){
                frontier[slot1] = tree[nodeIndex];
            }
        }
        //process slot2
        nodeIndex = leafCount + 2 ** 32;
        tree[nodeIndex] = output2[1];//cm
        if(slot2 == 0){
            frontier[0] = output2[1];
        }
        for (uint256 i = 1; i < slot2 + 1; i++) {
            nodeIndex = (nodeIndex - 1) / 2;
            tree[nodeIndex] = bytesToBytes32(msg, (i + slot1) * 32 - 30);
            if(i == slot2){
                frontier[slot2] = tree[nodeIndex];
            }
        }

        latestRoot = bytesToBytes32(msg, (slot1+slot2)*32+2);

        roots[latestRoot] = latestRoot;
        leafCount = leafCount + 2;

        nullifiers[nf] = nf;

        // cal delta time and get time, delta time is slot process
        (bool r2, bytes memory t2) = calTimeContract.call(abi.encode(startTime));
        require(r2, "getCurrentTime failed");
//        bytes8 startTimeSlot = bytesToBytes8(t2, 0);
    }

    //input_bytes32*10: cv, rk, spend_auth_sig, proof
    function burn(bytes32[10] calldata input, bytes32 anchor, bytes32 nf, uint64 value, bytes32[2] calldata bindingSignature, bytes32 signHash) external {

        require(value > 0, "Mint negative value.");
        require(nullifiers[nf] == 0, "The notecommitment being spent has already been nullified!");
        require(roots[anchor] != 0, "The anchor must exist");

        //bytes32 signHash = keccak256(abi.encode(address(this), msg.sender, spendDescription, payTo, value));
        // 512 = 384 + 64 + 32 + 32
        (bool result,bytes memory msg) = verifyProofContract.call(abi.encode(input, anchor, nf, value, bindingSignature, signHash));
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

    function bytesToBytes8(bytes memory b, uint offset) private returns (bytes8) {
        bytes8 out;
        for (uint i = 0; i < 8; i++) {
            out |= bytes8(b[i+offset] & 0xFF) >> (i * 8);
        }
        return out;
    }

    function bytesToUint64(bytes memory b) public returns (uint64) {
        require(b.length >= 8, "array too small");
        uint64 out;

        for (uint i = 0; i < 8; i++) {
            out |= uint8(b[i] & 0xFF) << (7-i);
        }

        return out;
    }

}
