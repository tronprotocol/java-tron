contract Event {
    event xixi(uint256 id) ;
    event log2(uint256,uint256,uint256);
    constructor() public payable{}
    function messageI() payable public returns (uint ret) {
        //emit log2(1,2,3);
        emit xixi(1);
        return 1;
    }
}