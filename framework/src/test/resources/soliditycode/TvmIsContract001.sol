contract testIsContract{
bool public isContrct;
constructor () public {
    isContrct = address(this).isContract;
}
function testIsContractCommand(address a) public returns (bool) {
return (a.isContract);
}

function testIsContractView(address a) view public returns (bool) {
return (a.isContract);
}

function selfdestructContract(address payable a) public {
    selfdestruct(a);
}
function testConstructor() public returns(bool){
    return isContrct;
}

function testConstructorView() public view returns(bool){
    return isContrct;
}
}
