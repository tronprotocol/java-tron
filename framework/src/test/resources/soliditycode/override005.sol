pragma solidity 0.6.0;

contract Base {
    enum ActionChoices { GoLeft, GoRight, GoStraight, SitStill }
    ActionChoices public choice2 = ActionChoices.GoRight;

    function stopped() virtual external pure returns (bool) {
        return true;
    }
    function i() virtual external pure returns (int) {
        return 32482980;
    }
    function i2() virtual external pure returns (int) {
        return -32482980;
    }
    function ui() virtual external pure returns (uint) {
        return 23487820;
    }
    function origin() virtual external pure returns (address) {
        return 0x3b0E4a6EdEE231CE0c3433F00F1bbc5FeD409c0B;
    }
    function b32() virtual external pure returns (bytes32) {
        return 0xb55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd231050;
    }
    function choice() virtual external returns (ActionChoices) {
        return choice2;
    }
}

contract Test is Base  {

    bool override public stopped = false;
    int override public i = 32482989;
    int override public i2 = -32482989;
    uint override public ui = 23487823;
    address override public origin = 0xdCad3a6d3569DF655070DEd06cb7A1b2Ccd1D3AF;
    bytes32 override public b32 = 0xb55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105c;
    ActionChoices override public choice = ActionChoices.SitStill;
}