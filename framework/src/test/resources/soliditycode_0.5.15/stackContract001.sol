pragma solidity ^0.5.0;

contract A{
    event log(uint256);
    constructor() payable public{
        emit log(withdrawreward());
        emit log(address(this).rewardbalance);
    }
    function withdrawRewardTest() public returns (uint256){
        return withdrawreward();
    }

    function test() public{
        emit log(123);
    }
}

contract B{
    event log(uint256);
    constructor() payable public{
        emit log(withdrawreward());
        emit log(address(this).rewardbalance);
    }
    function Stake(address sr, uint256 amount) public returns (bool result){
        return stake(sr, amount);
    }
    function UnStake() public returns (bool result){
        return unstake();
    }
    function SelfdestructTest(address payable target) public{
        selfdestruct(target);
    }
    function rewardBalance(address addr) public view returns (uint256){
        return addr.rewardbalance;
    }

    function nullAddressTest() public view returns (uint256) {
        return address(0x0).rewardbalance;
    }

    function localContractAddrTest() public view returns (uint256) {
        address payable localContract = address(uint160(address(this)));
        return localContract.rewardbalance;
    }

    function withdrawRewardTest() public returns (uint256){
        return withdrawreward();
    }

    function contractBWithdrawRewardTest(address contractB) public returns (uint) {
        return B(contractB).withdrawRewardTest();
    }

    function createA() public returns (address){
        return address(new A());
    }

    function callA(address Addr) public{
        A(Addr).test();
    }
}
