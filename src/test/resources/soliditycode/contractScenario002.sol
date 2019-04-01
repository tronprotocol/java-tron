pragma solidity ^0.4.0;
contract TronNative{

    address public voteContractAddress= 0x10001;
    address public freezeBalanceAddress = 0x10002;
    address public unFreezeBalanceAddress = 0x10003;
    address public withdrawBalanceAddress = 0x10004;
    address public approveProposalAddress = 0x10005;
    address public createProposalAddress = 0x10006;
    address public deleteProposalAddress = 0x10007;
    constructor () payable{}

    function voteForSingleWitness (address witnessAddr, uint256 voteValue) public{
        // method 1:
        voteContractAddress.delegatecall(witnessAddr,voteValue);
    }

    function voteUsingAssembly (address witnessAddr, uint256 voteValue) public{
        // method 2:
        assembly{
            mstore(0x80,witnessAddr)
            mstore(0xa0,voteValue)
            // gas, address, in, size, out, size
            if iszero(delegatecall(0, 0x10001, 0x80, 0x40, 0x80, 0x0)) {
                revert(0, 0)
            }
        }
    }

    function freezeBalance(uint256 frozen_Balance,uint256 frozen_Duration) public {
        freezeBalanceAddress.delegatecall(frozen_Balance,frozen_Duration);
    }

    function unFreezeBalance() public {
        unFreezeBalanceAddress.delegatecall();
    }

    function withdrawBalance() public {
        withdrawBalanceAddress.delegatecall();
    }

    function approveProposal(uint256 id, bool isApprove) public {
        approveProposalAddress.delegatecall(id,isApprove);
    }

    function createProposal(bytes32 [] data) public {
        createProposalAddress.delegatecall(data);
    }

    function deleteProposal(uint256 id) public{
        deleteProposalAddress.delegatecall(id);
    }
}