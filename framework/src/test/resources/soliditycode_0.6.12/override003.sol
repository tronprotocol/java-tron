pragma solidity ^0.6.0;
contract A {
    uint public x;
    function setValue(uint _x) public virtual {
        x = _x;
    }
}

contract B {
    uint public y;
    function setValue(uint _y) public virtual {
        y = _y;
    }
}

contract C is A, B {
    function setValue(uint _x) public override(B,A) {
        A.setValue(_x);
    }
}