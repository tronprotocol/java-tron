import "./SafeMath.sol";
pragma solidity ^0.5.8;
pragma experimental ABIEncoderV2;

interface ITRC20 {
    function transfer(address to, uint256 value) external returns (bool);
    function approve(address spender, uint256 value) external returns (bool);
    function transferFrom(address from, address to, uint256 value) external returns (bool);
    function totalSupply() external view returns (uint256);
    function balanceOf(address who) external view returns (uint256);
    function allowance(address owner, address spender) external view returns (uint256);
    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);
}

interface ITRCLP20 {
    function getTokenToTrxInputPrice(uint256 tokens_sold) external view returns (uint256);
    function tokenAddress() external view returns (ITRC20);
    function totalSupply() external view returns (uint256);
    function balanceOf(address account) external view returns (uint256);
    function transfer(address recipient, uint256 amount) external returns (bool);
    function allowance(address owner, address spender) external view returns (uint256);
    function approve(address spender, uint256 amount) external returns (bool);
    function transferFrom(address sender, address recipient, uint256 amount) external returns (bool);
    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);
}

interface TRC20LPPool{
    function earned(address account) external view returns (uint256);
    function totalSupply() external view returns (uint256);
    function balanceOf(address account) external view returns (uint256);
    // function rewardRate() external view returns (uint256);
    function rewardsPerSecond() external view returns (uint256);
    function tokenAddr() external view returns (ITRCLP20);
}

interface IStableSwap{
    function balances(uint256) external view returns (uint256);
    function token() external view returns(address);
}

interface ILpTokenStaker{
    struct UserInfo {
        uint256 amount;
        uint256 rewardDebt;
    }
    function userInfo(uint256,address)  external view returns(UserInfo memory);
    function poolLength() external view returns (uint256);
    function lockedSupply() external view returns(uint256);
}

contract ValuesAggregator {
    //SSP Exchange LP
    ITRCLP20 public sspTokenLP = ITRCLP20(0x388f4cb3F6927EaD070F4888eCbd3AE2977159a2);//todo:

    using SafeMath for uint256;
    struct tokenInfo{
        uint256 token_balance;
        uint256 token_allowance;
    }
    address public stableSwap;

    constructor(address _stableSwap) public{
        stableSwap = _stableSwap;
    }
   
    function getToken() public view returns (address) {
        return IStableSwap(stableSwap).token();
    }
    function getSwapBalance() public view returns(uint256[] memory balances){
        balances = new uint256[](3);
        for(uint256 i = 0; i < 3; i++){
            balances[i] = IStableSwap(stableSwap).balances(i);
        }
    }

    function getUserLP(address _user) public view returns(uint256 userLpBalance, uint256 lpTotalSupply,uint256 userLpAllowance,uint256[] memory balances){
        address token = getToken();
        userLpBalance = ITRC20(token).balanceOf(_user);
        lpTotalSupply = ITRC20(token).totalSupply();
        
        balances = new uint256[](3);
        for(uint256 i = 0; i < 3; i++){
            balances[i] = IStableSwap(stableSwap).balances(i);
        }
        userLpAllowance = ITRC20(token).allowance(_user,address(stableSwap));
    }

    // function fetchAPYofLpPool(address _lppooladdr)  public view returns( uint256 apy){
    //     TRC20LPPool trc20lpPool = TRC20LPPool(_lppooladdr);
    //     ITRCLP20 trc20lp = trc20lpPool.tokenAddr();
    //     // lp price(trx) = address(trc20lp).balance.mul(2) / trc20lp.totalSupply();
    //     // lp amount in trc20lpPool = trc20lpPool.totalSupply();
    //     uint256 lpTotal = trc20lpPool.totalSupply().mul(address(trc20lp).balance).mul(2).div(trc20lp.totalSupply());
    //     //ssp amountPerY
    //     uint256 amountPerY = trc20lpPool.rewardsPerSecond().mul(31_536_000);
    //     uint256 amountTrxPerY  = amountPerY * address(sspTokenLP).balance/sspTokenLP.tokenAddress().balanceOf(address(sspTokenLP));
    //     if(lpTotal > 0 ){
    //         apy = amountTrxPerY.mul(10**6).div(lpTotal);
    //     }   
    // }



    // function getSwapInfo() public view returns(uint256 _fee,uint256 _adminFee,uint256 _A){

    // }
    
   function getLpStaker(address _user,address _lp_staker) public view returns(uint256[] memory stakedBalance){
       uint256 length = ILpTokenStaker(_lp_staker).poolLength();
       stakedBalance = new uint256[](length);
       for(uint256 i = 0; i < length; i++){
         stakedBalance[i]  = ILpTokenStaker(_lp_staker).userInfo(i,_user).amount;
       }
   }

    function getHomeInfo(address sspToken,address _ssp_staker) public view returns(uint256 SspTotalSupply,uint256 lockedSupply){
        SspTotalSupply = ITRC20(sspToken).totalSupply();
        lockedSupply = ILpTokenStaker(_ssp_staker).lockedSupply();
    }


    function getBalanceAndApprove2(address _user , address[] memory _tokens , address[] memory _pools) public view returns(tokenInfo[] memory info){
        uint256 _tokenCount = _tokens.length;
        require(_tokenCount == _pools.length,'array length not matched');
        info = new tokenInfo[](_tokenCount);
        for(uint256 i = 0; i < _tokenCount; i++){
            uint256 token_amount = 0;
            uint256 token_allowance = 0;
            if(address(0) == _tokens[i]){
                token_amount = address(_user).balance;
                token_allowance = uint256(-1);
            }else{
                ( bool success, bytes memory data) = _tokens[i].staticcall(abi.encodeWithSelector(0x70a08231, _user));
                success;
                token_amount = 0;
                if(data.length != 0){
                    token_amount = abi.decode(data,(uint256));
                }
                token_allowance = ITRC20(_tokens[i]).allowance(_user,address(_pools[i]));
            }
            info[i] = tokenInfo(token_amount,token_allowance);
        }
    }


    function getBalanceAndApprove(address _user , address[] memory _tokens , address[] memory _pools) public view returns(uint256[] memory info, uint256[] memory _allowance){
        uint256 _tokenCount = _tokens.length;
        require(_tokenCount == _pools.length,'array length not matched');
        info = new uint256[](_tokenCount);
        for(uint256 i = 0; i < _tokenCount; i++){
            uint256 token_amount = 0;
            uint256 token_allowance = 0;
            if(address(0) == _tokens[i]){
                token_amount = address(_user).balance;
                token_allowance = uint256(-1);
            }else{
                ( bool success, bytes memory data) = _tokens[i].staticcall(abi.encodeWithSelector(0x70a08231, _user));
                success;
                token_amount = 0;
                if(data.length != 0){
                    token_amount = abi.decode(data,(uint256));
                }
                token_allowance = ITRC20(_tokens[i]).allowance(_user,address(_pools[i]));
            }
            info[i] = uint256(token_amount);
            _allowance[i] = uint256(token_allowance);
        }
    }


    function getBalance(address _user , address[] memory _tokens) public view returns(uint256[] memory info){
        uint256 _tokenCount = _tokens.length;
        info = new uint256[](_tokenCount);
        for(uint256 i = 0; i < _tokenCount; i++){
            uint256 token_amount = 0;
            if(address(0) == _tokens[i]){
                token_amount = address(_user).balance;
            }else{
                ( bool success, bytes memory data) = _tokens[i].staticcall(abi.encodeWithSelector(0x70a08231, _user));
                success;
                token_amount = 0;
                if(data.length != 0){
                    token_amount = abi.decode(data,(uint256));
                }
            }
            info[i] = uint256(token_amount);
        }
    }



}
