contract testIsContract{
    bool  result;
    constructor (bool a) public {
        result = a;
    }
function test( address a) public returns (bool) {
return result;
}
}