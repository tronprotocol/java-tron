
contract testA {
    constructor() public payable {
        A a = (new A).value(10)();
        a.fun();
    }
}

contract testB {
    constructor() public payable {
        B b = (new B).value(10)();
        b.fun();
    }
}


contract testC {
    constructor() public payable{
        C c = (new C).value(10)();
        c.fun();
    }
}

contract testD {
    constructor() public payable{
        D d = (new D).value(10)();
        d.fun();
    }
}


contract A {
    constructor() public payable{
        selfdestruct(msg.sender);
    }
    function fun() public {
    }

}

contract B {
    constructor() public payable {
        revert();
    }
    function fun() public {
    }
}


contract C {
    constructor() public payable {
       assert(1==2);
    }
    function fun() public {
    }
}

contract D {
    constructor() public payable {
       require(1==2);
    }
    function fun() public {
    }
}