
contract TestFreeze {
    constructor() public payable {}

    function freeze(address payable receiver, uint amount, uint res) external payable{
        receiver.freeze(amount, res);
    }

    function unfreeze(address payable receiver, uint res) external {
        receiver.unfreeze(res);
    }

    function destroy(address payable inheritor) external {
        selfdestruct(inheritor);
    }

    function send(address payable A) external {
        A.transfer(10);
    }

    function send(address payable A, uint256 value) external {
        A.transfer(value);
    }

    function getExpireTime(address payable target, uint res) external view returns(uint) {
        return target.freezeExpireTime(res);
    }

    function deploy(uint256 salt) public returns(address){
        address addr;
        bytes memory code = type(C).creationCode;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
        //if iszero(extcodesize(addr)) {
        //    revert(0, 0)
        //}
        }
        //emit Deployed(addr, salt, msg.sender);
        return addr;
    }

    function freezeAndSend(address payable receiver, uint amount, uint res) external {
        receiver.transfer(amount);
        receiver.freeze(amount, res);
    }


}


contract C {
    constructor() public payable {}

    function destroy(address payable inheritor) external {
        selfdestruct(inheritor);
    }
}

contract D {
    constructor() public payable {
        msg.sender.freeze(msg.value, 1);
    }
}