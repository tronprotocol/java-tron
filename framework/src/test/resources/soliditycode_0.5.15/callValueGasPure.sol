
contract C {
function check(address a) external pure returns (bool success) {
		a.call.value(42).gas(42);
		a.call.gas(42);
		//a.call.value(1).gas(42)("fwefewf");
}
}
