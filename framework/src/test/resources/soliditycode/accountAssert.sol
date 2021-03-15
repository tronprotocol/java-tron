pragma solidity ^0.6.0;

/**
在合约代码中，转trc10给旧的合约，看是否会新建AccountAssert 008---test1TransferToOldContractAddress
在合约里新建一个合约，查询新合约的Trc10资产 008---test1TransferToNewContract
在合约中，转trc10 给不存在的地址，查询新地址的Trc10资产 003---test3TransferTokenNonexistentTarget
在合约中，转trc10 给旧的地址，查询该地址的Trc10资产 008---test1TransferToOldContractAddress
suicide测试，判断AccountAssert是否被删掉。 004---test1SuicideNonexistentTarget
suicide并将接收者设为存在的地址 004---test2SuicideExistentTarget
suicide并将接收者设为不存在的地址，新地址有没有创建AccountAssert，并且有没有成功把本合约的trc10都转过去。 004---test1SuicideNonexistentTarget
合约里创建新的address后，revert后能否回退创建的AccountAssert 003---test9TransferTokenNonexistentTargetRevert
在构造函数里转trc10给不存在的地址/存在的地址 008---test2TransferToNonexistentTargetInConstructor/test3TransferToExistentTargetInConstructor
在构造函数里查询自己的trc10资产 008---test4GetTokenBalanceInConstructor
转给1个已经自杀的合约地址 004---test4transferToSuicideContract
*/
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