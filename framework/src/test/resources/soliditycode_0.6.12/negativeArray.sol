contract NegativeArray  {
    event LogNote(
    int64 a
    ) anonymous;

int64[]   b = [-1, 2, -3];

function set() public{
    b = [-1, 3,-8];
    emit LogNote(b[0]);
    emit LogNote(b[1]);
    emit LogNote(b[2]);

}


function get(uint a) public returns (int){
    return b[a];
}
}
