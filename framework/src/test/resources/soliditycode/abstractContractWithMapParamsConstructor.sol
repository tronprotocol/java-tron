abstract contract Feline {

    constructor (mapping (uint => uint) storage  m) {
        m[5] = 20;
    }

    function utterance() public virtual returns (bytes32);

    function getContractName() public returns (string memory){
        return "Feline";
    }
}


contract Cat is Feline {
    mapping (uint => uint) public m;

    constructor() Feline(m) {
    }
    function utterance() public override returns (bytes32) { return "miaow"; }
    function getMapValue() public returns (uint) { return m[5]; }

}