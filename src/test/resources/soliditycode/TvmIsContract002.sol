contract selfdestructCon{
function selfdestructContract(address a) public {
    selfdestruct(a);
}
}