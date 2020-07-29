pragma solidity ^0.4.24;

    contract A{
        uint256 public num = 0;
        constructor() public payable{}
        function transfer()  payable public{
            B b = (new B).value(10)();//1

        }
        function getBalance() returns(uint256){
            return this.balance;
        }
    }
    contract B{
        uint256 public num = 0;
        function f() payable returns(bool) {
            return true;
        }
        constructor() public payable {}
        function payC(address c, bool isRevert) public{
            c.transfer(1);//4
            if (isRevert) {
                revert();
            }
        }
        function getBalance() public returns(uint256){
            return this.balance;
        }
        function () payable{}
    }

