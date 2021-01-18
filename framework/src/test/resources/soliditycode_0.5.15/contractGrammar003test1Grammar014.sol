//pragma solidity ^0.4.4;
contract A {
uint256 public numberForB;
address public senderForB;
function callTest(address bAddress, uint256 _number) public{

//bAddress.call(bytes4(sha3("setValue(uint256)")), _number); // B's storage is set, A is not modified
bAddress.call(abi.encodeWithSignature("setValue(uint256)",_number)); // B's storage is set, A is not modified
}
function callcodeTest(address bAddress, uint256 _number) public{
//bAddress.callcode(bytes4(sha3("setValue(uint256)")), _number); // A's storage is set, B is not modified
bAddress.delegatecall(abi.encodeWithSignature("setValue(uint256)", _number)); // A's storage is set, B is not modified
}
function delegatecallTest(address bAddress, uint256 _number) public{
//bAddress.delegatecall(bytes4(sha3("setValue(uint256)")), _number); // A's storage is set, B is not modified
bAddress.delegatecall(abi.encodeWithSignature("setValue(uint256)", _number)); // A's storage is set, B is not modified
}

function callAddTest(address bAddress) public{
//bAddress.call(bytes4(sha3("add()"))); // B's storage is set, A is not modified
bAddress.call(abi.encodeWithSignature("add()")); // B's storage is set, A is not modified
//bAddress.call(bytes4(sha3("add()"))); // B's storage is set, A is not modified
bAddress.call(abi.encodeWithSignature("add()")); // B's storage is set, A is not modified
}
function getnumberForB() public returns(uint256){
        return numberForB;
    }
    function getsenderForB() public returns(address){
        return senderForB;
    }
}
contract B {
uint256 public numberForB;
address public senderForB;
address public addr11;
mapping(uint256=>address) public addr1;
mapping(uint256=>address) public addr2;
event ssss(uint256);
function setValue(uint256 _number) public{

emit ssss(_number);
numberForB = _number;
senderForB = msg.sender;
// senderForB is A if invoked by A's callTest. B's storage will be updated
// senderForB is A if invoked by A's callcodeTest. None of B's storage is updated
// senderForB is OWNER if invoked by A's delegatecallTest. None of B's storage is updated
}

function add() public{
numberForB=numberForB+1;
C c1 = new C();
addr1[numberForB]=c1.getAddress();
addr11 = c1.getAddress();
C c2 = new C();
addr2[numberForB] = c2.getAddress();
}
function getnumberForB() public returns(uint256){
        return numberForB;
    }
    function getsenderForB() public returns(address){
        return senderForB;
    }
}
contract C {
function getAddress() public view returns(address){
return address(this);
}
}