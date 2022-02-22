

contract transferTestContract {
    function tranferTest(address payable addr) public payable{
        addr.transfer(10);

    }
}