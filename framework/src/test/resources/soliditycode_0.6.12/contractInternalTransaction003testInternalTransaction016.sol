

    contract A{
        uint256 public num = 0;
        constructor() public payable{}
        fallback() payable external{}
        function transfer()  payable public{
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            B  b1=(new B).value(1)();//1
            address payable aaa=address(this);
            b1.suicide1(aaa);
        }
        function transfer2()  payable public{
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            (new B).value(1)();//1
            B  b1=(new B).value(1)();//1
            address payable aaa=address(this);
            b1.suicide1(aaa);
        }
        function getBalance() public  returns(uint256){
            return address(this).balance;
        }
    }
    contract B{
        uint256 public num = 0;
        function f() payable public returns(bool) {
            return true;
        }
        constructor() public payable {}
        function payC(address payable c, bool isRevert) public{
            c.transfer(1);//4
            if (isRevert) {
                revert();
            }
        }
        function getBalance() public returns(uint256){
            return address(this).balance;
        }
        fallback() payable external{}
        function suicide1(address payable toAddress) public payable{
        selfdestruct(toAddress);
    }
    }

