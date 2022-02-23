pragma solidity ^0.5.0;
contract testStakeSuicide{
    B b;
    constructor() payable public{}
    function deployB() payable public returns (B addrB){
        b = (new B).value(1000000000)();
        return b;
    }
    function SelfdestructTest(address payable target) public{
        selfdestruct(target);
    }
    function SelfdestructTest2(address sr, uint256 amount, address payable target) public{
        stake(sr, amount);
        selfdestruct(target);
    }
    function Stake(address sr, uint256 amount) public payable returns (bool result){
        return stake(sr, amount);
    }
    function Stake2(address sr, uint256 amount) public returns (bool result){
        stake(sr, amount);
        return stake(sr, amount);
    }
    function UnStake() public returns (bool result){
        return unstake();
    }
    function UnStake2() public returns (bool result){
        unstake();
        return unstake();
    }
    function WithdrawReward() public {
        withdrawreward();
    }
    function RewardBalance(address addr) view public returns (uint256 balance) {
        return addr.rewardbalance;
    }
    function revertTest1(address sr, uint256 amount, address payable transferAddr) public{
        transferAddr.transfer(1000000);
        stake(sr, amount);
        transferAddr.transfer(2000000);
        stake(sr, 1000000000000000);//stake more than balance to fail
        transferAddr.transfer(4000000);
    }
    function revertTest2(address payable transferAddr) public{
        transferAddr.transfer(1000000);
        unstake();
        transferAddr.transfer(2000000);
        unstake();//unstake twice to fail
        transferAddr.transfer(4000000);
    }

    function BStake(address sr, uint256 amount) public returns (bool result){
        return b.Stake(sr, amount);
    }
    function BUnStake() public returns (bool result){
        return b.UnStake();
    }
    function transfer(address payable add,uint256 num) public {
        return add.transfer(num);
    }
}

contract B{
    constructor() payable public{}
    function Stake(address sr, uint256 amount) public returns (bool result){
        return stake(sr, amount);
    }
    function UnStake() public returns (bool result){
        return unstake();
    }
    function SelfdestructTest(address payable target) public{
        selfdestruct(target);
    }

    function deploy(bytes memory code, uint256 salt) public returns(address) {
        address addr;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        return addr;
    }
}