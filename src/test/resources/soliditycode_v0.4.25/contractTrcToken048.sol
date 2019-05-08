//pragma solidity ^0.4.24;

 contract Test {
		    event log(uint256);
 		    function testMsgTokenValue() payable public returns(uint256 value) {
		        emit log(msg.tokenvalue);
 		        return msg.tokenvalue;
 		    }

 		    function testMsgValue() payable public returns(uint256 value) {
 		        emit log(msg.value);
 		        return msg.value;
		    }
     		}