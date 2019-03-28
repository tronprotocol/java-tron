pragma solidity ^0.4.24;

 contract Test {
		    event log(uint256);
 		    function testMsgTokenValue() payable returns(uint256 value) {
		        log(msg.tokenvalue);
 		        return msg.tokenvalue;
 		    }

 		    function testMsgValue() payable returns(uint256 value) {
 		        log(msg.value);
 		        return msg.value;
		    }
     		}