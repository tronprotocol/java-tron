
type TestInt128 is int128;
pragma abicoder v2;
interface I {
function foo() external;
}

contract C is I {
uint8 immutable i;
uint8 x;

constructor() {
i = 33;
x = readI();
}

function readX() public view returns (uint8) {
return x;
}

function readI() public view returns (uint8) {
return i;
}




function fExternal(uint256 p, string memory t) external {}

function fSignatureFromLiteralCall() public view returns (bytes memory) {
return abi.encodeCall(this.fExternal, (1, "123"));
}





enum FreshJuiceSize{ SMALL, MEDIUM, LARGE, FINAL }


TestInt128 a = TestInt128.wrap(45);
function getUserDefinedValue() public view returns(TestInt128) {
return a;
}




function foo() external  { // Does not compile without override
}

function getEnumMin() public view returns (FreshJuiceSize)  {
return type(FreshJuiceSize).min;

}


function getEnumMax() public view returns (FreshJuiceSize)  {
return type(FreshJuiceSize).max;

}


function testFunction() external {}

function testYul1() public view returns (address adr) {
function() external fp = this.testFunction;

assembly {
adr := fp.address
}
}
function testGetAddress() public view returns (address) {
return this.testFunction.address;
}
function testYul2() public view returns (uint32) {
function() external fp = this.testFunction;
uint selectorValue = 0;

assembly {
selectorValue := fp.selector
}

// Value is right-aligned, we shift it so it can be compared
return uint32(bytes4(bytes32(selectorValue << (256 - 32))));
}
function testGetSelector() public view returns (uint32) {
return uint32(this.testFunction.selector);
}



int8 immutable bugValue = -4;
function fixBugTest() public view returns (bytes32 r) {
int8 y = bugValue;
assembly { r := y }
}



}