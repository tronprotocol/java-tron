


contract A {
    constructor() public payable{
    }

    fallback() external payable {
    }
}

contract PayableTest {

address payable a1;
function receiveMoneyTransfer(address a, uint256 _x) public {
a1 = payable(a);
a1.transfer(_x);
}

function receiveMoneySend(address a, uint256 x) public {
address payable a2 = payable(a);
a2.send(x);
}

function receiveMoneyTransferWithContract(A PayableTest, uint256 x) public {
payable(address(PayableTest)).transfer(x);
}

constructor() public payable{
}
}