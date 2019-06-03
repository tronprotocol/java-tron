contract Counter {
uint count = 0;
address payable owner;
event LogResult(bytes32 _hashBefore, bytes32 _hashAfter);
constructor() public{
owner = msg.sender;
}
function getCodeHashByAddr() public returns (bytes32 _hashBefore, bytes32 _hashAfter) {
address addr = address(this);
assembly {
_hashBefore := extcodehash(addr)
}
if (owner == msg.sender) {
selfdestruct(owner);
}
assembly {
_hashAfter := extcodehash(addr)
}
revert();
emit LogResult(_hashBefore, _hashAfter);
}
}

contract Counter1 {
uint count = 0;
address payable owner;
event LogResult(bytes32 _hashBefore, bytes32 _hashAfter);
constructor() public{
owner = msg.sender;
}
function getCodeHashByAddr() public returns (bytes32 _hashBefore, bytes32 _hashAfter) {
address addr = address(this);
assembly {
_hashBefore := extcodehash(addr)
}
if (owner == msg.sender) {
selfdestruct(owner);
}
assembly {
_hashAfter := extcodehash(addr)
}

emit LogResult(_hashBefore, _hashAfter);
}
}


contract Counter2 {
uint count = 0;
address payable owner;
event LogResult(bytes32 _hashBefore, bytes32 _hashAfter);
constructor() public{
owner = msg.sender;
}
function getCodeHashByAddr(address c) public returns (bytes32 _hashBefore, bytes32 _hashAfter) {
    TestConstract t=new TestConstract();
address addr = address(t);
assembly {
_hashBefore := extcodehash(addr)
}
  addr.call(abi.encodeWithSignature("testSuicideNonexistentTarget(address)",c));


assembly {
_hashAfter := extcodehash(addr)
}

emit LogResult(_hashBefore, _hashAfter);
}
}


contract Counter3 {
uint count = 0;
address payable owner;
event LogResult(bytes32 _hashBefore, bytes32 _hashAfter);
constructor() public{
owner = msg.sender;
}
function getCodeHashByAddr(address c) public returns (bytes32 _hashBefore, bytes32 _hashAfter) {
    TestConstract t=new TestConstract();
address addr = address(t);
assembly {
_hashBefore := extcodehash(addr)
}
if (owner == msg.sender) {
selfdestruct(owner);
}

assembly {
_hashAfter := extcodehash(addr)
}

emit LogResult(_hashBefore, _hashAfter);
}
}

contract TestConstract {
    uint public i=1;
    function testSuicideNonexistentTarget(address payable nonexistentTarget) payable public {
         selfdestruct(nonexistentTarget);
    }
}