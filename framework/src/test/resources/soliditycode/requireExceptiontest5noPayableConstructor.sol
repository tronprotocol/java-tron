

contract MyContract {
    uint money;

    //function MyContract(uint _money) {
    constructor(uint _money) public payable{
        require(msg.value >= _money);
        money = _money;
    }
}