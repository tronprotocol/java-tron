//pragma solidity ^0.4.0;

contract ToMathedFeed {
    uint public i=1;
    function ToMathed (uint value) public {
         i=value;
    }
}

contract ToMathedUseINContract {
    function ToMathedIUseNR(address a,uint256 n) public returns(bool,bytes memory){
        address payContract=a;
        return payContract.call(abi.encode(bytes4(keccak256("ToMathedNot(uint256)")),n));
    }
    function ToMathedIUseNRE(address a,uint256 value) public returns(bool,bytes memory){
        address payContract=a;
        return payContract.call(abi.encode(bytes4(keccak256("ToMathed(uint256)")),value));
    }
}