//pragma solidity ^0.4.24;

contract IllegalDecorate {

    constructor() payable public{}

    function() payable external{}

    function transferTokenWithView(address payable toAddress,trcToken id, uint256 tokenValue) public view{

        toAddress.transferToken(tokenValue, id);

    }

}