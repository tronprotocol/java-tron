contract Event {
    event log2(uint256,uint256,uint256);
    constructor() public payable{}
    function messageI() payable public returns (uint ret) {
        emit log2(1,1,1);
        return 1;
    }
}