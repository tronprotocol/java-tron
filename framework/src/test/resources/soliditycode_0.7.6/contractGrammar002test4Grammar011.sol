
contract C {
function f(uint key, uint value) public returns(uint) {
return key;
// do something
}
function g() public {
// named arguments
f({value: 2, key: 3});
}
}