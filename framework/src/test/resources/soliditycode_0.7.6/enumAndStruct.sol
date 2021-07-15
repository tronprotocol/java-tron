

struct S_out {
uint x;
}

enum ErrorType {
Revert_Error,    //0
RevertWithMsg_Error,    //1
Require_Error,    //2
RequirewithMsg_Error,    //3
Assert_Error,    //4
Tansfer_Error,    //5
Send_Error,    //6
Math_Error,    //7
ArrayOverFlow_Error     //8
}

contract enumAndStructTest {

struct S_inner {
int x;
}

enum ErrorType_inner {
Revert_Error,    //0
RevertWithMsg_Error,    //1
Require_Error,    //2
RequirewithMsg_Error,    //3
Assert_Error,    //4
Tansfer_Error,    //5
Send_Error,    //6
Math_Error,    //7
ArrayOverFlow_Error     //8
}

function getvalue() public returns(uint) {
    require(ErrorType.Require_Error == ErrorType(2));
    S_out memory s = S_out(1);
    return s.x;
}

}