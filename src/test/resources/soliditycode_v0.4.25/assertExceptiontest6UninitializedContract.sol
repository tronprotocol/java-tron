pragma solidity ^0.4.0;
contract uni {
function b(int x, int y) internal  returns (int)
{
    return x * y;
}

function test1() external  returns (int)
{
    // Variable containing a function pointer
    function (int, int) internal  returns (int) funcPtr;

    funcPtr = b;

    // This call to funcPtr will succeed
    return funcPtr(4, 5);
}

function test2() external returns (int)
{
    // Variable containing a function pointer
    function (int, int) internal returns (int) funcPtr;

    // This call will fail because funcPtr is still a zero-initialized function pointer
    return funcPtr(4, 5);
}
}