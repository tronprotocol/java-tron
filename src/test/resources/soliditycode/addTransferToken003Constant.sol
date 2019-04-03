//pragma solidity ^0.4.24;

contract IllegalDecorate {

    constructor() payable public{}

    function() payable external{}

     function transferTokenWithConstant(address payable toAddress, uint256 tokenValue) public constant{

        toAddress.transferToken(tokenValue, 0x6e6d62);

    }

}