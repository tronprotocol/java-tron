pragma solidity ^0.6.0;

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
contract errorContract {
    uint256[]  arraryUint ;

    function errorSwitch(uint256 errorType) public returns(string memory) {
        if (ErrorType(errorType) == ErrorType.Revert_Error){
            revert();
        } else if (ErrorType(errorType) == ErrorType.RevertWithMsg_Error){
            revert("Revert Msg.");
        } else if (ErrorType(errorType) == ErrorType.Require_Error) {
            require(0>1);
        } else if (ErrorType(errorType) == ErrorType.RequirewithMsg_Error) {
            require(0>1,"Require Msg.");
        } else if (ErrorType(errorType) == ErrorType.Assert_Error) {
            assert(1<0);
        } else if (ErrorType(errorType) == ErrorType.Tansfer_Error) {
            payable(msg.sender).transfer(1);
        } else if (ErrorType(errorType) == ErrorType.Send_Error) {
            payable(msg.sender).send(1);
        } else if (ErrorType(errorType) == ErrorType.Math_Error) {
            uint256 a = 1;
            uint256 b = 0;
            uint256 n = a / b;
        } else if (ErrorType(errorType) == ErrorType.ArrayOverFlow_Error) {
            arraryUint.pop();
        }
        return "success";

    }

    function callFun(string memory functionStr, string memory argsStr) public{
        address(this).call(abi.encodeWithSignature(functionStr, argsStr));
    }

}

contract NewContract {
    uint256[]  arraryUint ;

    constructor(uint256 errorType) public payable{
        if (ErrorType(errorType) == ErrorType.Revert_Error){
            revert();
        } else if (ErrorType(errorType) == ErrorType.RevertWithMsg_Error){
            revert("Revert Msg.");
        } else if (ErrorType(errorType) == ErrorType.Require_Error) {
            require(0>1);
        } else if (ErrorType(errorType) == ErrorType.RequirewithMsg_Error) {
            require(0>1,"Require Msg.");
        } else if (ErrorType(errorType) == ErrorType.Assert_Error) {
            assert(1<0);
        } else if (ErrorType(errorType) == ErrorType.Tansfer_Error) {
            payable(msg.sender).transfer(1);
        } else if (ErrorType(errorType) == ErrorType.Send_Error) {
            payable(msg.sender).send(1);
        } else if (ErrorType(errorType) == ErrorType.Math_Error) {
            uint256 a = 1;
            uint256 b = 0;
            uint256 n = a / b;
        } else if (ErrorType(errorType) == ErrorType.ArrayOverFlow_Error) {
            arraryUint.pop();
        }
    }
}

contract tryTest {
    function getData(errorContract inter, string memory functionStr, string memory argsStr) public payable returns(string memory) {
        try inter.callFun(functionStr,argsStr) {
            return "123";
        } catch Error(string memory errorMsg/* 出错原因 */) {
            return errorMsg;
        } catch (bytes memory) {
            return "3";
        }
    }

    function getErrorSwitch(errorContract add, uint256 errorType ) public payable returns(string memory) {
        try add.errorSwitch(errorType) returns (string memory Msg) {
            return Msg;
        } catch Error(string memory errorMsg/* 出错原因 */) {
            return errorMsg;
        } catch (bytes memory) {
            return "NoErrorMsg";
        }
    }

    function catchNewErrorSwitch(uint256 errorType) public returns (address nc){
        try new NewContract(errorType) returns (NewContract nc){
            return address(nc);
        }catch {
            return address(0x00);
        }
    }
}