pragma experimental ABIEncoderV2;

contract validatemultisignTest {
    function testmulti(address a, uint256 perid, bytes32 hash, bytes[] memory signatures) public returns (bool){
        return validatemultisign(a, perid, hash, signatures);
    }

    function testbatch(bytes32 hash, bytes[] memory signatures, address[] memory addresses) public returns (bytes32){
        return batchvalidatesign(hash, signatures, addresses);
    }

    function testMultiPrecompileContract(bytes memory data) public returns(bool, bytes memory){
        return address(0xa).delegatecall(data);
    }
}