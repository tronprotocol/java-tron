pragma solidity ^0.4.24;

contract transferTrc10 {
    function receive(address rec) public payable {
        uint256 aamount=address(this).tokenBalance(msg.tokenid);
        uint256 bamount=rec.tokenBalance(msg.tokenid);
        require(msg.tokenvalue==aamount);
        require(aamount==msg.tokenvalue);
        rec.transferToken(aamount,msg.tokenid);
        require(0==address(this).tokenBalance(msg.tokenid));
        require(bamount+aamount==rec.tokenBalance(msg.tokenid));
        require(rec.call(bytes4(keccak256("checkTrc10(uint256,trcToken,uint256)")),bamount+aamount,msg.tokenid,0));
    }
}

contract receiveTrc10 {
    function() public payable {
    }
    function checkTrc10(uint256 amount,trcToken tid,uint256 meamount) public{
        require(amount==address(this).tokenBalance(tid));
        require(meamount==msg.sender.tokenBalance(tid));
    }
}