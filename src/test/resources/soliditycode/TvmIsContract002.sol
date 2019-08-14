contract testIsContract{
function testIsContractCommand(address a) public returns (bool) {
return (a.isContract);
}
}
