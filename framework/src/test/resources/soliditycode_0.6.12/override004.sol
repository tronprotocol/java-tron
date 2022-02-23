pragma solidity >=0.5.0 <0.7.0;

contract A {
    uint public x = 4;
    function setValue(uint _x) public notZero {
        x = _x;
    }
    modifier notZero() virtual {
        require(x >= 5,"x must >= 5");
        _;
    }
}

contract B is A {
    function setValue2(uint _x) public {
    x = _x;
    }
}

contract C is A,B {
    modifier notZero override {
        require(x >= 6,"x must >= 6");
        _;
    }
}
