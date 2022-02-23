

// version 0.6.0  change
// add abstract and override
abstract contract Feline {

 function utterance() public virtual returns (bytes32);

 function getContractName() public returns (string memory){
  return "Feline";
 }
}


contract Cat is Feline {
 function utterance() public override returns (bytes32) { return "miaow"; }

}