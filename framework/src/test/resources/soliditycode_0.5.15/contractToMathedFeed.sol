//pragma solidity ^0.4.0;

contract ToMathedFeed {
    uint public i=1;
    function ToMathed (uint value) public {
         i=value;
    }
}

contract ToMathedUseINContract {
    function ToMathedIUseNR(address a,uint256 n) public returns(bool){
        address payContract=a;
        (bool success, bytes memory data) = payContract.call(abi.encodeWithSignature("ToMathedNot(uint256)",n));
        return success;
    }
    function ToMathedIUseNRE(address a,uint256 value) public returns(bool){
        address payContract=a;
        (bool success, bytes memory data) = payContract.call(abi.encodeWithSignature("ToMathed(uint256)",value));
        return success;
    }
}