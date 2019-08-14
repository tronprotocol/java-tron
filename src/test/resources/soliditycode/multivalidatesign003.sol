pragma experimental ABIEncoderV2;

contract Demo {
bool public result;
constructor (bytes32 hash, bytes[] memory signatures, address[] memory addresses) public {
    result = multivalidatesign(hash, signatures, addresses);
}
function testConstructor() public returns(bool){
    return result;
}
}