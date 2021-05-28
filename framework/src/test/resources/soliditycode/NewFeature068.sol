// SPDX-License-Identifier: GPL-3.0
//pragma solidity ^0.6.8;

abstract contract testModifier {
    modifier isOwner() virtual;
}

abstract contract testInterfaceId {
    function getValue() external view virtual returns(uint);
    function getOwner() external view virtual returns(uint);

}

interface a {
    function getValue() external view returns(uint);
    function getOwner() external view returns(uint);
}

contract testMapKey is testModifier{

    enum size{
        SMALL,
        LARGE
    }

    mapping(size => uint) public enums;

    mapping(testMapKey => uint) public contracts;

    function setEnumValue(uint value) public {
        enums[size.SMALL] = value;
    }

    function getEnumValue() public view returns(uint) {
        return enums[size.SMALL];
    }

    function setContractValue() public {
        contracts[this] = 2;
    }

    function getContractValue() public view returns(uint) {
        return contracts[this];
    }

    bytes4 constant functionSelector = this.getEnumValue.selector;

    function getfunctionSelector() public pure returns(bytes4) {
        return functionSelector;
    }

    uint immutable x;
    address immutable owner = msg.sender;

    constructor() public {
        x = 5;
    }

    string b = "test";

    function testStorage() public view returns(string memory) {
        string storage aa;
        aa = b;
        return aa;

    }

    function getImmutableVal() public view returns(uint) {
        return x;
    }

    function getOwner() public view returns(address) {
        return owner;
    }

    function getInterfaceId() public pure returns(bytes4,bytes4) {
        return (type(a).interfaceId, type(testInterfaceId).interfaceId);
    }

    modifier isOwner() override {
        require(msg.sender == owner);
        _;
    }

    function requireOwner() public view isOwner returns(uint) {
        return 6;
    }


    function getUint256MinAndMax() public pure returns(uint, uint) {
        return (type(uint).min, type(uint).max);
    }


    function getUint8MinAndMax() public pure returns(uint8, uint8) {
        return (type(uint8).min, type(uint8).max);
    }

}


abstract contract base {
    function abstractfun() virtual public returns(uint);
}

abstract contract callEmptyFunction is base {
    function callfun() public returns(uint) {
        return abstractfun();
    }
}
















