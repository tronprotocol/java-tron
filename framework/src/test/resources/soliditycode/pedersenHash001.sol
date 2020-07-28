pragma experimental ABIEncoderV2;

contract pedersenHashTest {

    function test1() public returns (bool, bytes memory){
        bytes memory empty = "";
        return address(0x1000004).delegatecall(empty);
    }

    function test2(bytes memory data) public returns (bool, bytes memory){
        return address(0x1000004).delegatecall(data);
    }

    function test3(uint32 hash, bytes32 left, bytes32 right) public returns (bytes32){
        return pedersenHash(hash, left, right);
    }

}