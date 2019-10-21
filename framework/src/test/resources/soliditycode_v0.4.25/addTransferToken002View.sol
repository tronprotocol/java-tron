//pragma solidity ^0.4.24;

contract IllegalDecorate {

    constructor() payable public{}

    function() payable public{}

    function transferTokenWithView(address toAddress, uint256 tokenValue) public view {

        toAddress.transferToken(tokenValue, 0x6e6d62);

    }

}