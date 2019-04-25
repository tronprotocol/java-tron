pragma solidity ^0.4.0;
contract TestThrowsContract{
    function testAssert(){
        assert(1==2);
    }
    function testRequire(){
        require(2==1);
    }
    function testRevert(){
        revert();
    }
    function testThrow(){
        throw;
    }
}