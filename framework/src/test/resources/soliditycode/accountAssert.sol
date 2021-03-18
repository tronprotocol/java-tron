pragma solidity ^0.6.0;

contract transferTokenTestA {

    // transfer trc10 to a new address or exist address in constructor
    constructor(address payable toAddress, uint256 tokenValue, trcToken id) payable public{
        toAddress.transferToken(tokenValue, id);
        require(toAddress.tokenBalance(id) > 0, "tokenBalance should not be 0");
    }

    fallback() payable external{}

    function transferTest(address payable toAddress, uint256 tokenValue) payable public    {
        toAddress.transfer(tokenValue);
    }

    function transferTokenTest(address payable toAddress, uint256 tokenValue, trcToken id) payable public    {
        toAddress.transferToken(tokenValue, id);
    }

    // suicide to a new address
    function selfdestructTest(address payable toAddress) payable public{
        selfdestruct(toAddress);
    }

    // transfer to a new contract
    function createContractTest(uint256 tokenValue, trcToken id) payable public returns(address){
        Simple s = new Simple();
        require(address(s).tokenBalance(id)==0, "tokenBalance should be 0");
        address(s).transferToken(tokenValue, id);
        require(address(s).tokenBalance(id)==tokenValue, "tokenBalance should not be 0");
        return address(s);
    }

    // revert transfer to a new contract
    function revertCreateContractTest(uint256 tokenValue, trcToken id) payable public {
        Simple s = new Simple();
        address(s).transferToken(tokenValue, id);
        revert();
    }
}

contract transferTokenTestB {

    constructor() payable public{
    }

    fallback() payable external{}

    function transferTest(address payable toAddress, uint256 tokenValue) payable public    {
        toAddress.transfer(tokenValue);
    }

    function transferTokenTest(address payable toAddress, uint256 tokenValue, trcToken id) payable public    {
        toAddress.transferToken(tokenValue, id);
    }

    // suicide to a new address
    function selfdestructTest(address payable toAddress) payable public{
        selfdestruct(toAddress);
    }

    // transfer to a new contract
    function createContractTest(uint256 tokenValue, trcToken id) payable public returns(address){
        Simple s = new Simple();
        require(address(s).tokenBalance(id)==0, "tokenBalance should be 0");
        address(s).transferToken(tokenValue, id);
        require(address(s).tokenBalance(id)==tokenValue, "tokenBalance should not be 0");
        return address(s);
    }

    // revert transfer to a new contract
    function revertCreateContractTest(uint256 tokenValue, trcToken id) payable public {
        Simple s = new Simple();
        address(s).transferToken(tokenValue, id);
        revert();
    }
}

contract transferTokenTestC {
    Simple public s;

    // transfer to a new address in constructor
    constructor(trcToken id) payable public{
        s = new Simple();
        require(address(s).tokenBalance(id)==0, "new contract tokenBalance should be 0");
        require(address(this).tokenBalance(id)==0, "this.tokenBalance should be 0");
    }
}

contract Simple {
    constructor() payable public{}
    fallback() payable external{}
}