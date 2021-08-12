//pragma solidity ^0.6.0;

contract selectorContract {
    function testSelectorNoParam() external pure returns(uint) {
        return 11;
    }

    function testSelectorWithParam(uint x) external pure returns(uint) {
        return 22;
    }
}

interface interfaceSelector {
    function getSelector() external pure returns(uint);
}

interface B is interfaceSelector {
    // interface现在可以继承自其他interface
    function testImplemention() external pure returns(uint);
}

contract implementContract is B{
    function getSelector() external override pure returns(uint) {
        return 66;
    }

    function testImplemention() external override pure returns(uint) {
        return 77;
    }

    constructor() public payable {}
}

contract basicContract{
    function testNewUse() external payable returns(uint) {
        return 345;
    }

    constructor() public payable {}
}

contract TestGasValue{
    constructor() public payable {}

    function testNewUse() external payable returns(uint) {
        return 123;
    }
    basicContract bc = new basicContract();
    // external方法在调用时可以采用c.f{gas: 10000, value: 4 trx}()的形式
    function callWithGasAndValue(uint x,uint y) external returns(uint) {
        return bc.testNewUse{gas:x, value:y}();
    }

    function callThisNoGasAnd1Value() external returns(uint) {
        return this.testNewUse{gas:0, value:1}();
    }

    // inline assembly中允许true和false字面量
    function testAssemblyTrue() public pure returns(uint x) {
        assembly {
            x := true
        }
    }

    // inline assembly中允许true和false字面量
    function testAssemblyFalse() public pure returns(uint x) {
        assembly {
            x := false
        }
    }

    // create2的high-level用法new C{salt: 0x1234, value: 1 ether}(arg1, arg2)
    function testCreate2() public returns(address) {
        basicContract c = new basicContract{salt: bytes32(bytes1(0x01)), value: 1 trx}();
        return address(c);
    }


    function getContractSelectorNoParam() public pure returns(bytes4) {
        return selectorContract.testSelectorNoParam.selector;
    }

    function getContractSelectorWithParam() public pure returns(bytes4) {
        return selectorContract.testSelectorWithParam.selector;
    }

    function getInterfaceSelectorNoParam() public pure returns(bytes4) {
        return interfaceSelector.getSelector.selector;
    }

}

