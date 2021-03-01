pragma solidity ^0;

library A {
    function getBalance(address) public view returns (uint256) {
        return address(this).balance;
    }

    function getamount(address) external view returns (uint256) {
        return address(this).balance;
    }
}

contract testSelector {
    using A for address;


    function getselector2() public view returns (bytes4, bytes4) {
        return (A.getBalance.selector, A.getamount.selector);
    }

}