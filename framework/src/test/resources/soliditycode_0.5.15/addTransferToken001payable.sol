//pragma solidity ^0.4.24;

 contract IllegalDecorate {

    constructor() payable public{}

    function() payable external{}

    function transferTokenWithOutPayable(address payable toAddress,trcToken id, uint256 tokenValue) public payable{

        toAddress.transferToken(tokenValue, id);
    }
}