

contract transferTrc10 {
    function receive(address payable rec) public payable {
        uint256 aamount=address(this).tokenBalance(msg.tokenid);
        uint256 bamount=rec.tokenBalance(msg.tokenid);
        require(msg.tokenvalue==aamount);
        require(aamount==msg.tokenvalue);
        rec.transferToken(aamount,msg.tokenid);
        //require(rec.call(abi.encode(bytes4(keccak256("AssertError()")))));
        (bool suc, bytes memory data) = rec.call(abi.encodeWithSignature("AssertError()"));
        require(suc);
        require(aamount==address(this).tokenBalance(msg.tokenid));
        require(bamount==rec.tokenBalance(msg.tokenid));
    }
}

contract receiveTrc10 {
    fallback() external payable {
    }
    function AssertError() public{
        assert(1==2);
    }
}