pragma solidity ^0.4.24;

contract transferTrc10 {
    function receive(address rec) public payable {
        uint256 aamount=address(this).tokenBalance(msg.tokenid);
        uint256 bamount=rec.tokenBalance(msg.tokenid);
        require(msg.tokenvalue==aamount);
        require(aamount==msg.tokenvalue);
        rec.transferToken(aamount,msg.tokenid);
        require(rec.call(bytes4(keccak256("AssertError()"))));
        require(aamount==address(this).tokenBalance(msg.tokenid));
        require(bamount==rec.tokenBalance(msg.tokenid));
    }
}

contract receiveTrc10 {
    function() public payable {
    }
    function AssertError() public{
        assert(1==2);
    }
}