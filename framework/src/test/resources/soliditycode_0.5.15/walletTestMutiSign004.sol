contract timeoutTest {
        string public iarray1;
    // cpu
    function oneCpu() public {
        require(1==1);
    }

    function storage8Char() public {
        iarray1 = "12345678";
    }

    function testUseCpu(uint256 a) public returns (uint256){
        uint256 count = 0;
        for (uint256 i = 0; i < a; i++) {
            count++;
            }
        return count;
    }


    uint256[] public iarray;
    uint public calculatedFibNumber;
    mapping(address=>mapping(address=>uint256)) public m;

    function testUseStorage(uint256 a) public returns (uint256){
        uint256 count = 0;
        for (uint256 i = 0; i < a; i++) {
            count++;
            iarray.push(i);
            }
        return count;
    }

    // stack
    //uint n = 0;
    uint yy = 0;
    function test() public {
        //n += 1;
        yy += 1;
        test();
    }

    function setFibonacci(uint n) public returns (uint256){
        calculatedFibNumber = fibonacci(n);
        return calculatedFibNumber;
    }

    function fibonacci(uint n) internal returns (uint) {
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
}