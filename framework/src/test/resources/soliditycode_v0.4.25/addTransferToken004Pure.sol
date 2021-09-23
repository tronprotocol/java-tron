//pragma solidity ^0.4.24;

contract IllegalDecorate {

    constructor() payable public{}

    function() payable public{}

     function transferTokenWithPure(address toAddress, uint256 tokenValue) public pure {

        toAddress.transferToken(tokenValue, 0x6e6d62);

    }

}