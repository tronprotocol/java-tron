
contract Test {
    address public origin;
    address public sender;
    bool public result1;
    bool public result2;
    function test() external {
        origin = tx.origin;
        sender = msg.sender;
        result1 = msg.sender == tx.origin; // true
        result2 = origin == sender; // true
    }
function getResult1() public returns(bool){
    return result1;
}
function getResult2() public returns(bool){
    return result2;
}
}