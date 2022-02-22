

contract Test {
    fallback() external { x = 1; }
    uint x;
}


contract Caller {
    function callTest(Test test) public {
               //test.call(0xabcdef01); // hash does not exist
               address(test).call(abi.encode(0xabcdef01)); // hash does not exist
    }
}