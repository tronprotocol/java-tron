
contract Test {
    byte[] a;

    function ChangeSize() external returns(byte[] memory) {
        a.push(0x01);
        a.length = 3;

        a.length ++;
        a.length --;
        a.length --;

        a.pop();
        return a;
    }
}