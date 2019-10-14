//pragma solidity ^0.4.0;
contract TronNative{

    address public voteContractAddress= address(0x10001);
    address public freezeBalanceAddress = address(0x10002);
    address public unFreezeBalanceAddress = address(0x10003);
    address public withdrawBalanceAddress = address(0x10004);
    address public approveProposalAddress = address(0x10005);
    address public createProposalAddress = address(0x10006);
    address public deleteProposalAddress = address(0x10007);
    constructor () payable public {}

    function voteForSingleWitness (address witnessAddr, uint256 voteValue) public{
        // method 1:
        voteContractAddress.delegatecall(abi.encode(witnessAddr,voteValue));
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
        freezeBalanceAddress.delegatecall(abi.encode(frozen_Balance,frozen_Duration));
    }

    function unFreezeBalance() public {
        unFreezeBalanceAddress.delegatecall("");
    }

    function withdrawBalance() public {
        withdrawBalanceAddress.delegatecall("");
    }

    function approveProposal(uint256 id, bool isApprove) public {
        approveProposalAddress.delegatecall(abi.encode(id,isApprove));
    }

    function createProposal(bytes32 [] memory data) public {
        createProposalAddress.delegatecall(abi.encode(data));
    }

    function deleteProposal(uint256 id) public{
        deleteProposalAddress.delegatecall(abi.encode(id));
    }
}