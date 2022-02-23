pragma solidity ^0.6.0;

interface X {
    function setValue(uint _x) external;
    function setBalance(uint _x) external;
}

abstract contract abstract001 is X {
    uint x;
    function setX(uint _x) public { x = _x; }
}