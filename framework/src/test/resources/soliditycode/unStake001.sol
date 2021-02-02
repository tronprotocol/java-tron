

contract unStakeTest {
    B b;
    constructor() payable public{}
    function deployB() payable public returns (B addrB){
        b = (new B).value(1000000000)();
        return b;
    }

    function selfdestructTest(address payable target) public {
        selfdestruct(target);
    }

    function selfdestructTest2(address sr, uint256 amount, address payable target) public {
        stake(sr, amount);
        selfdestruct(target);
    }

    function Stake(address sr, uint256 amount) public returns (bool result){
        return stake(sr, amount);
    }

    function stake2(address sr, uint256 amount) public returns (bool result){
        stake(sr, amount);
        return stake(sr, amount);
    }

    function unStake() public returns (bool result){
        return unstake();
    }

    function unStake2() public returns (bool result){
        unstake();
        return unstake();
    }

    function withdrawReward() public returns (uint256 amount) {
        return withdrawreward();
    }

    function rewardBalance(address addr) view public returns (uint256 balance) {
        return addr.rewardbalance;
    }

    function revertTest1(address sr, uint256 amount, address payable transferAddr) public {
        transferAddr.transfer(1000000);
        stake(sr, amount);
        transferAddr.transfer(2000000);
        stake(sr, 1000000000000000);
        //stake more than balance to fail
        transferAddr.transfer(4000000);
    }

    function revertTest2(address payable transferAddr) public {
        transferAddr.transfer(1000000);
        unstake();
        transferAddr.transfer(2000000);
        unstake();
        //unstake twice to fail
        transferAddr.transfer(4000000);
    }

    function BStake(address sr, uint256 amount) public returns (bool result){
        return b.Stake(sr, amount);
    }

    function BUnStake() public returns (bool result){
        return b.UnStake();
    }

    function BSelfdestructTest(address payable target) public {
        b.SelfdestructTest(target);
    }
}

contract B {
    constructor() payable public{}
    function Stake(address sr, uint256 amount) public returns (bool result){
        return stake(sr, amount);
    }

    function UnStake() public returns (bool result){
        return unstake();
    }

    function SelfdestructTest(address payable target) public {
        selfdestruct(target);
    }
}