contract Counter {
    uint count = 0;
    address payable owner;
    event LogResult(bytes32 _hashBefore, bytes32 _hashAfter);
    constructor() public{
        owner = msg.sender;
    }
    function getCodeHashSuicide(address addr) public returns (bytes32 _hashBefore){
        assembly{
            _hashBefore := extcodehash(addr)
        }
        selfdestruct(owner);
        return _hashBefore;
    }

    function getCodeHashRevert() public returns (bytes32 _hashBefore, bytes32 _hashAfter) {
        address addr = address(this);
        assembly {
            _hashBefore := extcodehash(addr)
        }
        if (owner == msg.sender) {
            selfdestruct(owner);
        }
        assembly {
            _hashAfter := extcodehash(addr)
        }
        revert();
        emit LogResult(_hashBefore, _hashAfter);
    }

    function getCodeHashCreate() public  returns (bytes32 _hashBefore){
        TestContract A = (new TestContract).value(0)();
        address addr = address(A);
        assembly{
                    _hashBefore := extcodehash(addr)
                }
        revert();
        return _hashBefore;
    }
}

contract TestContract{
    uint256 count = 1;
    constructor() public payable{
    }
}