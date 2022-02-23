
pragma solidity ^0.5.0;

contract ContractB{
    address others;
}

contract TestIsSRCandidate{

    ContractB contractB = new ContractB();

    function isSRCandidateTest(address addr) public view returns (bool) {
        return address(addr).isSRCandidate;
    }

    function zeroAddressTest() public view returns (bool) {
        return address(0x0).isSRCandidate;
    }

    function localContractAddrTest() public view returns (bool) {
        return address(this).isSRCandidate;
    }

    function otherContractAddrTest() public view returns (bool) {
        return address(contractB).isSRCandidate;
    }

    function nonpayableAddrTest(address addr) public view returns (bool) {
        return addr.isSRCandidate;
    }

    function payableAddrTest(address payable addr) public returns (bool) {
        return addr.isSRCandidate;
    }
}