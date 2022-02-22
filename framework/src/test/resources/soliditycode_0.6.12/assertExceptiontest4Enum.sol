

contract enumContract {
    enum ActionChoices { GoLeft, GoRight, GoStraight, SitStill }
    ActionChoices _choice;
    function setGoStraight(ActionChoices choice) public {
        _choice = choice;
    }

    function getChoice()  public returns (ActionChoices) {
        return _choice;
    }
}