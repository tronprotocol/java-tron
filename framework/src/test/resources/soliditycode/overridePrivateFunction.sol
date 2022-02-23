pragma solidity ^0.5.17;

contract A {

    function test() private pure returns(uint) {
        return 1;
    }

}

contract B is A {

    function basic() private pure returns(uint) {
        return 2;
    }
    function testOverridePrivate() external payable returns(uint) {
        return basic();
    }

    constructor() public payable {}
}

