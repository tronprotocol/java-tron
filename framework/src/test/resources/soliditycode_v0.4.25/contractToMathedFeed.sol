//pragma solidity ^0.4.0;

contract ToMathedFeed {
    uint public i=1;
    function ToMathed(uint value) {
         i=value;
    }
}

contract ToMathedUseINContract {
    function ToMathedIUseNR(address a,uint256 n) returns(bool){
        address payContract=a;
        return payContract.call(bytes4(keccak256("ToMathedNot(uint256)")),n);
    }
    function ToMathedIUseNRE(address a,uint256 value) returns(bool){
        address payContract=a;
        return payContract.call(bytes4(keccak256("ToMathed(uint256)")),value);
    }
}