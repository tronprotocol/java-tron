pragma solidity ^0.4.0;
contract CrowdFunding{
struct Funder{
address addr;
uint amount;
}

struct Campaign{
address beneficiary;
uint goal;
uint amount;
uint funderNum;
mapping(uint => Funder) funders;
}

uint compaingnID;
mapping (uint => Campaign) campaigns;

function candidate(address beneficiary, uint goal) returns (uint compaingnID){
// initialize
campaigns[compaingnID++] = Campaign(beneficiary, goal, 0, 0);
}

function vote(uint compaingnID) payable {
Campaign c = campaigns[compaingnID];

//another way to initialize
c.funders[c.funderNum++] = Funder({addr: msg.sender, amount: msg.value});
c.amount += msg.value;
}

function check(uint comapingnId) returns (bool){
Campaign c = campaigns[comapingnId];

if(c.amount < c.goal){
return false;
}

uint amount = c.amount;
// incase send much more
c.amount = 0;
if(!c.beneficiary.send(amount)){
throw;
}
return true;
}
}