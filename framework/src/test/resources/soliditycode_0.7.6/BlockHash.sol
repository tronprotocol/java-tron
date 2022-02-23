contract TestBlockHash {

    function testOR1(bytes32 value) public returns(bytes32, bytes32, bytes32) {
        bytes32 b1 = blockhash(block.number - 1);
        bytes32 c =  blockhash(block.number - 1) | bytes32(value);
        return (b1, c, blockhash(block.number - 1));
    }

    function testOR2(bytes32 value) public returns(bytes32, bytes32, bytes32) {
        bytes32 b1 = blockhash(block.number - 1);
        bytes32 c = bytes32(value) | blockhash(block.number - 1);
        return (b1, c, blockhash(block.number - 1));
    }

    function testAND1(bytes32 value) public returns(bytes32, bytes32, bytes32) {
        bytes32 b1 = blockhash(block.number - 1);
        bytes32 c =  blockhash(block.number - 1) & bytes32(value);
        return (b1, c, blockhash(block.number - 1));
    }

    function testAND2(bytes32 value) public returns(bytes32, bytes32, bytes32) {
        bytes32 b1 = blockhash(block.number - 1);
        bytes32 c = bytes32(value) & blockhash(block.number - 1);
        return (b1, c, blockhash(block.number - 1));
    }

    function testXOR1(bytes32 value) public returns(bytes32, bytes32, bytes32) {
        bytes32 b1 = blockhash(block.number - 1);
        bytes32 c =  blockhash(block.number - 1) ^ bytes32(value);
        return (b1, c, blockhash(block.number - 1));
    }

    function testXOR2(bytes32 value) public returns(bytes32, bytes32, bytes32) {
        bytes32 b1 = blockhash(block.number - 1);
        bytes32 c = bytes32(value) ^ blockhash(block.number - 1);
        return (b1, c, blockhash(block.number - 1));
    }
}