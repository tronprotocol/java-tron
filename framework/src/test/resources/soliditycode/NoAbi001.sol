contract testNoABiContract{
    uint public i = 2;
    event trigger(uint256 i, address sender);

    function testTrigger() public returns (uint) {
        i++;
        emit trigger(i, msg.sender);
        return i;
    }
}