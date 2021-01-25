
contract CrowdFunding{
struct Funder{
address addr;
uint amount;
}

struct Campaign{
address payable beneficiary;
uint goal;
uint amount;
uint funderNum;
mapping(uint => Funder) funders;
}

uint compaingnID;
mapping (uint => Campaign) campaigns;

function candidate(address payable beneficiary, uint goal) public payable returns (uint compaingnID){
// initialize
campaigns[compaingnID++] = Campaign(beneficiary, goal, 0, 0);
}

function vote(uint compaingnID) payable public {
Campaign storage c = campaigns[compaingnID];

//another way to initialize
c.funders[c.funderNum++] = Funder({addr: msg.sender, amount: msg.value});
c.amount += msg.value;
}

function check(uint comapingnId) public payable returns (bool){
    Campaign memory c = campaigns[comapingnId];

    if(c.amount < c.goal){
    return false;
    }

    uint amount = c.amount;
    // incase send much more
    c.amount = 0;
  //  address payable addr = address(uint160(c.beneficiary));
    //if(! addr.send(amount)){

    if (! c.beneficiary.send(amount)){
    revert();
    }
    return true;
}
}