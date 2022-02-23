pragma solidity ^0.6.0;
interface X {
    function setValue(uint _x) external;
}
abstract contract Y {
    function setBool(bool _y) external virtual ;
}
contract Y2 {
    string public z;
    function setString(string calldata _z) external virtual { z = "123"; }
}

contract Z is X,Y,Y2 {
    uint public x;
    bool public y;
    function setValue(uint _x) external override { x = _x; }
    function setBool(bool _y) external override { y = _y; }
    function setString(string calldata _z) external override { z = _z; }
}
