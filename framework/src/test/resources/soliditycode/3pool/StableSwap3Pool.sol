pragma solidity ^0.5.8;
pragma experimental ABIEncoderV2;

import "./SafeMath.sol";
import "./ITRC20.sol";
import "./TransferHelper.sol";
import "./ReentrancyGuard.sol";


interface FeeConverter {
    function setFeeDistributor(address distributor) external;

    function convertFees(uint i, uint j) external;

    function notify(address coin) external;
}


interface CurveToken {
    function totalSupply() external view returns (uint256);

    // function mint(address _to, uint256 _value) external returns (bool);
    function mint(address _to, uint256 _value) external;

    function burnFrom(address _to, uint256 _value) external;
}

contract StableSwap3Pool is ReentrancyGuard{
    using SafeMath for uint256;
    using TransferHelper for address;

    event TokenExchange (
        address indexed buyer,
        int128 sold_id,
        uint256 tokens_sold,
        int128 bought_id,
        uint256 tokens_bought
    );

    event AddLiquidity (
        address indexed provider,
        uint256[N_COINS] token_amounts,
        uint256[N_COINS] fees,
        uint256 invariant,
        uint256 token_supply
    );

    event RemoveLiquidity(
        address indexed provider,
        uint256[N_COINS] token_amounts,
        uint256[N_COINS] fees,
        uint256 token_supply
    );

    event RemoveLiquidityOne(
        address indexed provider,
        uint256 token_amount,
        uint256 coin_amount
    );

    event RemoveLiquidityImbalance(
        address indexed provider,
        uint256[N_COINS] token_amounts,
        uint256[N_COINS] fees,
        uint256 invariant,
        uint256 token_supply
    );

    event CommitNewAdmin(
        uint256 indexed deadline,
        address indexed admin
    );

    event NewAdmin(
        address indexed admin
    );

    event NewFeeConverter(
        address indexed fee_converter
    );

    event CommitNewFee(
        uint256 indexed deadline,
        uint256 fee,
        uint256 admin_fee
    );

    event NewFee(
        uint256 fee,
        uint256 admin_fee
    );

    event RampA(
        uint256 old_A,
        uint256 new_A,
        uint256 initial_time,
        uint256 future_time
    );

    event StopRampA(
        uint256 A,
        uint256 t
    );

    //ALERT: CHANGE WITH N_COINS !
    uint256 constant ZEROS256 = 0;
    uint256 constant N_COINS = 3;
    uint256[N_COINS] ZEROS = [ZEROS256, ZEROS256, ZEROS256];
    //

    uint256 constant FEE_DENOMINATOR = 10 ** 10;
    uint256 constant LENDING_PRECISION = 10 ** 18;
    uint256 constant PRECISION = 10 ** 18;
    uint256[N_COINS] PRECISION_MUL = [1, 1, 1];
    uint256[N_COINS] RATES = [1000000000000000000, 1000000000000000000, 1000000000000000000];
    uint128 constant FEE_INDEX = 2;

    uint256 constant MAX_ADMIN_FEE = 10 * 10 ** 9;
    uint256 constant MAX_FEE = 5 * 10 ** 9;
    uint256 constant MAX_A = 10 ** 6;
    uint256 constant MAX_A_CHANGE = 10;

    uint256 constant ADMIN_ACTIONS_DELAY = 3 * 86400;
    uint256 constant MIN_RAMP_TIME = 86400;

    address[N_COINS] public coins;
    uint256[N_COINS] public balances;
    uint256 public fee;
    uint256 public admin_fee;

    address public owner;
    CurveToken public token;

    uint256 public initial_A;
    uint256 public future_A;
    uint256 public initial_A_time;
    uint256 public future_A_time;

    uint256 public admin_actions_deadline;
    uint256 public transfer_ownership_deadline;
    uint256 public future_fee;
    uint256 public future_admin_fee;
    address public future_owner;

    bool is_killed;
    uint256 kill_deadline;
    uint256 constant KILL_DEADLINE_DT = 2 * 30 * 86400;

    address public fee_converter;

    function() external payable {}

    constructor(
        address _owner,
        address[N_COINS] memory _coins,
        address _pool_token,
        uint256 _A,
        uint256 _fee,
        uint256 _admin_fee,// 10位精度，admin抽成交易费的百分比
        address _fee_converter
    ) public {
        for (uint i = 0; i < _coins.length; i++) {
            require(_coins[i] != address(0));
        }
        coins = _coins;
        initial_A = _A;
        future_A = _A;
        fee = _fee;
        admin_fee = _admin_fee;
        owner = _owner;
        kill_deadline = block.timestamp + KILL_DEADLINE_DT;
        token = CurveToken(_pool_token);
        fee_converter = _fee_converter;
    }

    function _A() internal view returns (uint256){
        uint256 t1 = future_A_time;
        uint256 A1 = future_A;
        if (block.timestamp < t1) {
            uint256 A0 = initial_A;
            uint256 t0 = initial_A_time;
            if (A1 > A0) {
                return A0.add(A1.sub(A0).mul(block.timestamp.sub(t0)).div(t1.sub(t0)));
            } else {
                return A0.sub(A0.sub(A1).mul(block.timestamp.sub(t0)).div(t1.sub(t0)));
            }
        } else {
            return A1;
        }
    }

    function A() external view returns (uint256){
        return _A();
    }


    function _xp() public view returns (uint256[N_COINS] memory){
        uint256[N_COINS] memory result = RATES;
        for (uint i = 0; i < N_COINS; i++) {
            result[i] = result[i].mul(balances[i]).div(LENDING_PRECISION);
        }
        return result;
    }


    function _xp_mem(uint256[N_COINS] memory _balances) public returns (uint256[N_COINS] memory){
        uint256[N_COINS] memory result = RATES;
        for (uint i = 0; i < N_COINS; i++) {
            result[i] = result[i].mul(_balances[i]).div(PRECISION);
        }
        return result;
    }


    function get_D(uint[N_COINS] memory xp,uint amp) public pure returns (uint256) {
        uint256 S = 0;
        for (uint j = 0; j < xp.length; j++) {
            S = S.add(xp[j]);
        }
        if( S == 0 ){
            return 0;
        }
        uint Dprev = 0;
        uint D = S;
        uint Ann = amp.mul(xp.length);
        for (uint i = 0; i < 255; i++) {
            uint D_P = D;
            for(uint k = 0;k<xp.length; k++){
                D_P = D_P.mul(D).div(xp[k].mul(N_COINS));
            }
            Dprev = D;
            D = Ann.mul(S).add(D_P.mul(N_COINS)).mul(D).div(Ann.sub(1).mul(D).add(N_COINS.add(1).mul(D_P)));
            if( D > Dprev){
                if (D.sub(Dprev) <= 1) {
                    break;
                }
            }else{
                if (Dprev.sub(D) <= 1) {
                    break;
                }
            }
        }
        return D;
    }


    function get_D_mem(uint256[N_COINS] memory _balances, uint256 amp) public returns (uint256){
        return get_D(_xp_mem(_balances), amp);
    }


    function get_virtual_price() public view returns (uint256){
        uint256 D = get_D(_xp(), _A());
        uint256 token_supply = token.totalSupply();
        return D.mul(PRECISION).div(token_supply);
    }


    function calc_token_amount(uint256[N_COINS] calldata amounts, bool deposit) external returns (uint256){
        uint256[N_COINS] memory _balances = balances;
        uint256 amp = _A();
        uint256 D0 = get_D_mem(_balances, amp);
        for (uint i = 0; i < N_COINS; i++) {
            if (deposit) {
                _balances[i] = _balances[i].add(amounts[i]);
            } else {
                _balances[i] = _balances[i].sub(amounts[i]);
            }
        }
        uint256 D1 = get_D_mem(_balances, amp);
        uint256 token_amount = token.totalSupply();
        uint256 diff = 0;
        if (deposit) {
            diff = D1.sub(D0);
        } else {
            diff = D0.sub(D1);
        }
        return diff.mul(token_amount).div(D0);
    }


    // 没有手续费
    function get_y(uint128 i, uint128 j,uint256 x, uint256[N_COINS] memory xp_) internal view returns(uint256){
        require(i != j, "same coin");
        require(j >= 0, "j below zero");
        require(j < N_COINS, "j above N_COINS");

        require( i >= 0);
        require(i < N_COINS);

        uint256 amp = _A();
        uint256 D = get_D(xp_, amp);
        uint256 c = D;
        uint256 S_ = 0;
        uint256 Ann = amp.mul(N_COINS);

        uint256 _x = 0;
        for(uint256 _i = 0; _i < N_COINS; _i++){
            if( _i == i){
                 _x = x;
            }else if( _i != j ){
                _x = xp_[_i];
            }else{
                continue;
            }
            S_ = S_.add(_x);
            c = c.mul(D).div(_x.mul(N_COINS));
        }
        c = c.mul(D).div(Ann.mul(N_COINS));
        uint256 b = S_.add(D.div(Ann));
        uint256 y_prev = 0;
        uint256 y = D;
        for(uint256 _i = 0; _i < 255; _i++){
            y_prev = y;
            y = y.mul(y).add(c).div(y.mul(2).add(b).sub(D));
            if(y > y_prev){
                    if(y - y_prev <= 1){
                        break;
                    }
                else{
                    if(y_prev - y <= 1){
                        break;
                    }
                }
            }
        }
        return y;
    }

    // 包含手续费
    function get_dy(uint128 i, uint128 j,uint256 dx) external view returns(uint256){
        // uint256[N_COINS] memory rates = RATES;
        uint256[N_COINS] memory xp = _xp();
        uint256 dy;
        {   
            uint256 x = xp[i].add(dx.mul(RATES[i]).div(PRECISION)) ;
            uint256 y = get_y(i, j, x, xp);
            dy = (xp[j].sub(y).sub(1)).mul(PRECISION).div(RATES[j]);
        }
        uint256 _fee = fee.mul(dy).div(FEE_DENOMINATOR);

        return dy.sub(_fee);
    }

    // 没有用
    function get_dy_underlying(uint128 i, uint128 j,uint256 dx) external view returns(uint256){
        uint256[N_COINS] memory xp = _xp();
        uint256[N_COINS] memory precisions = PRECISION_MUL;

        uint256 x = xp[i].add(dx.mul(precisions[i]));
        uint256 y = get_y(i, j, x, xp);
        uint256 dy = (xp[j].sub(y).sub(1)).div(precisions[j]);
        uint256 _fee = fee.mul(dy).div(FEE_DENOMINATOR);

        return dy.sub(_fee);
    }

    function exchange(uint128 i, uint128 j, uint256 dx, uint256 min_dy) external  nonReentrant {
        require(!is_killed,"is killed");
        uint256[N_COINS] memory rates = RATES;
        uint256[N_COINS] memory old_balances = balances;
        uint256[N_COINS] memory xp = _xp_mem(old_balances);

        uint256 dx_w_fee = dx;
        address input_coin = coins[i];
        if(i == FEE_INDEX){
            dx_w_fee = ITRC20(input_coin).balanceOf(address(this));
        }

        require(address(input_coin).safeTransferFrom(msg.sender, address(this), dx),"failed transferfrom");

        if(i == FEE_INDEX){
            dx_w_fee = ITRC20(input_coin).balanceOf(address(this)).sub(dx_w_fee);
        }

        uint256 y;
        {
            uint256 x = xp[i].add(dx_w_fee.mul(rates[i]).div(PRECISION));
            y = get_y(i, j, x, xp);
        }


        uint256 dy = xp[j].sub(y).sub(1);
        uint256 dy_fee = dy.mul(fee).div(FEE_DENOMINATOR);
        {
        dy = (dy.sub(dy_fee)).mul(PRECISION).div(rates[j]);
        require(dy >= min_dy, "Exchange resulted in fewer coins than expected");

        uint256 dy_admin_fee = dy_fee.mul(admin_fee).div(FEE_DENOMINATOR);
        dy_admin_fee = dy_admin_fee.mul(PRECISION).div(rates[j]);

        balances[i] = old_balances[i] + dx_w_fee;
        // balances[j] = old_balances[j].sub(dy).sub(dy_admin_fee);
        balances[j] = old_balances[j] -dy - dy_admin_fee;

    }
        require(address(coins[j]).safeTransfer(msg.sender,dy),"failed transfer");

        emit TokenExchange(msg.sender, int128(i), dx, int128(j), dy);

    }


    function add_liquidity(uint256[N_COINS] calldata amounts, uint256 min_mint_amount) external nonReentrant {
        require(!is_killed, "is killed");

        uint256 amp = _A();

        uint256 token_supply = token.totalSupply();
        // Initial invariant
        uint256 D0 = 0;
        uint256[N_COINS] memory old_balances = balances;

        if (token_supply > 0) {
            D0 = get_D_mem(old_balances, amp);
        }

        uint256[N_COINS] memory new_balances = old_balances;
        for (uint256 i = 0; i < N_COINS; i++)
        {
            uint256 in_amount = amounts[i];
            if (token_supply == 0) {
                require(in_amount > 0, "in_amount must gt 0");
            }
            address in_coin = coins[i];

            // Take coins from the sender
            if (in_amount > 0) {
                if (i == FEE_INDEX) {
                    in_amount = ITRC20(in_coin).balanceOf(address(this));
                }

                require(address(coins[i]).safeTransferFrom(msg.sender, address(this), amounts[i]), "failed transfer");

                if (i == FEE_INDEX) {
                    in_amount = ITRC20(in_coin).balanceOf(address(this)).sub(in_amount);
                }
            }

            new_balances[i] = old_balances[i].add(in_amount);
        }


        uint256 D1 = get_D_mem(new_balances, amp);
        require(D1 > D0, "D1 must gt D0");
        uint256 mint_amount = 0;
        {
            uint256[N_COINS] memory fees = ZEROS;
            uint256 D2 = D1;
            if (token_supply > 0) {
                uint256 _fee = fee.mul(N_COINS).div((N_COINS - 1) * 4);
                uint256 _admin_fee = admin_fee;
                for (uint256 i = 0; i < N_COINS; i++) {
                    uint256 ideal_balance = D1.mul(old_balances[i]).div(D0);
                    uint256 difference = 0;

                    if (ideal_balance > new_balances[i]) {
                        difference = ideal_balance.sub(new_balances[i]);
                    } else {
                        difference = new_balances[i].sub(ideal_balance);
                    }
                    fees[i] = _fee.mul(difference).div(FEE_DENOMINATOR);
                    balances[i] = new_balances[i].sub(fees[i].mul(_admin_fee).div(FEE_DENOMINATOR));
                    new_balances[i] = new_balances[i].sub(fees[i]);
                }
                D2 = get_D_mem(new_balances, amp);
            } else {
                balances = new_balances;
            }
            if (token_supply == 0) {
                mint_amount = D1;
            } else {
                mint_amount = token_supply.mul(D2.sub(D0)).div(D0);
            }


            require(mint_amount >= min_mint_amount, "Slippage screwed you");
            //Mint pool tokens
            token.mint(msg.sender, mint_amount);

            emit  AddLiquidity(msg.sender, amounts, fees, D1, token_supply.add(mint_amount));

        }
    }

    function remove_liquidity(uint256 _amount, uint256[N_COINS] calldata min_amounts) external nonReentrant{
        uint256 total_supply = token.totalSupply();
    
        // uint256[] memory amounts = new uint256[]();
        // uint256[] memory fees = new uint256[](N_COINS);
        uint256[N_COINS] memory amounts = ZEROS;
        uint256[N_COINS] memory fees = ZEROS;

        for(uint256 i = 0; i < N_COINS; i++){
            uint256 value = balances[i].mul(_amount).div(total_supply);
            require(value >= min_amounts[i], "Withdrawal resulted in fewer coins than expected");
            balances[i] = balances[i].sub(value);
            amounts[i] = value;

            require(address(coins[i]).safeTransfer(msg.sender,value),"failed transfer");

        }
        token.burnFrom(msg.sender, _amount);
        emit RemoveLiquidity(msg.sender, amounts, fees, total_supply.sub(_amount));
    }

    function remove_liquidity_imbalance(uint256[N_COINS] calldata amounts, uint256 max_burn_amount) external nonReentrant{
        require(!is_killed,"is killed");
        uint256 token_supply = token.totalSupply();
        require(token_supply != 0 ,"zero total supply");
        uint256 _fee = fee.mul(N_COINS).div((N_COINS - 1) * 4);

        uint256 _admin_fee = admin_fee;
        uint256 amp = _A();

        uint256[N_COINS] memory old_balances = balances;
        uint256[N_COINS] memory new_balances = old_balances;
        uint256 D0 = get_D_mem(old_balances, amp);

        for(uint256 i = 0; i < N_COINS; i++){
            new_balances[i] =new_balances[i].sub(amounts[i]);
        }
        uint256 D1 = get_D_mem(new_balances, amp);
        // uint256[] memory fees =  new uint256[](N_COINS);
        uint256[N_COINS] memory fees = ZEROS;

        for(uint256 i = 0; i < N_COINS; i++){
            uint256 ideal_balance =  D1.mul(old_balances[i]).div(D0);
            uint256 difference = 0;
            if(ideal_balance > new_balances[i]){
                difference = ideal_balance.sub(new_balances[i]);
            }else{
                difference = new_balances[i].sub(ideal_balance);
            }
            fees[i] = _fee.mul(difference).div(FEE_DENOMINATOR);
            balances[i] = new_balances[i].sub((fees[i].mul(_admin_fee).div(FEE_DENOMINATOR)));
            new_balances[i] =new_balances[i].sub(fees[i]);
        }
        uint256 token_amount;
        {
            uint256 D2 = get_D_mem(new_balances, amp);
            token_amount = (D0.sub(D2)).mul(token_supply).div(D0);
        }
        require(token_amount <= max_burn_amount, "Slippage screwed you");
        token.burnFrom(msg.sender, token_amount);

        for(uint256 i = 0; i < N_COINS; i++){
            if(amounts[i] != 0){
            require(address(coins[i]).safeTransfer(msg.sender,amounts[i]),"failed transfer");
            }
        }

        emit RemoveLiquidityImbalance(msg.sender, amounts, fees, D1, token_supply - token_amount);
    }

    function get_y_D(uint256 A_, uint128 i, uint256[N_COINS] memory xp, uint256 D) internal view returns(uint256){
      /**
        Calculate x[i] if one reduces D from being calculated for xp to D

        Done by solving quadratic equation iteratively.
        x_1**2 + x1 * (sum' - (A*n**n - 1) * D / (A * n**n)) = D ** (n + 1) / (n ** (2 * n) * prod' * A)
        x_1**2 + b*x_1 = c

        x_1 = (x_1**2 + c) / (2*x_1 + b)
       */

      // x in the input is converted to the same price/precision
      require(i >= 0 , "i below zero");
      require(i < N_COINS, "i above N_COINS");

      uint256 c = D;
      uint256 S_ = 0;
      uint256 Ann = A_.mul(N_COINS);

      uint256 _x = 0;

      for(uint256 _i = 0; _i < N_COINS; _i++){
          if(_i != i){
             _x = xp[_i];
          }else{
            continue;
          }
          S_ = S_.add(_x);
          c = c.mul(D).div(_x.mul(N_COINS));
        }
        c = c.mul(D).div(Ann.mul(N_COINS));
        uint256 b = S_.add(D.div(Ann));

        uint256 y_prev = 0;
        uint256 y = D;

        for(uint256 _i = 0; _i < 255; _i++){
            y_prev = y;
            y = y.mul(y).add(c).div(y.mul(2).add(b).sub(D));
            if( y > y_prev){
                if(y - y_prev <= 1){
                    break;
                }
            }else{
                if(y_prev - y <= 1){
                    break;
                }
            }
        }

        return y;
    }

    function _calc_withdraw_one_coin(uint256 _token_amount, uint128 i) internal view  returns(uint256,uint256){
        // First, need to calculate
        //* Get current D
        //* Solve Eqn against y_i for D - _token_amount
        uint256 amp = _A();
        uint256 _fee = fee.mul(N_COINS).div((N_COINS - 1) * 4);
        // uint256[N_COINS] memory precisions = PRECISION_MUL;
        uint256 total_supply = token.totalSupply();

        uint256[N_COINS] memory xp = _xp();
        uint256 D0 = get_D(xp, amp);
        uint256 D1 = D0.sub(_token_amount.mul(D0).div(total_supply));
        uint256[N_COINS] memory xp_reduced = xp;

        uint256 new_y = get_y_D(amp, i, xp, D1);
        uint256 dy_0 = (xp[i].sub(new_y)) / (PRECISION_MUL[i]);  // w/o fees

        for(uint256 j; j < N_COINS; j++){
            uint256 dx_expected = 0;
            if(j == i){
                dx_expected = xp[j].mul(D1).div(D0).sub(new_y);
            }else{
                dx_expected = xp[j].sub(xp[j].mul(D1).div(D0));
            }
            xp_reduced[j] =xp_reduced[j].sub(_fee.mul(dx_expected).div(FEE_DENOMINATOR));
        }

        uint256 dy = xp_reduced[i].sub(get_y_D(amp, i, xp_reduced, D1));
        dy = (dy - 1).div(PRECISION_MUL[i]);

        return (dy, dy_0.sub(dy));
    }

    function calc_withdraw_one_coin(uint256 _token_amount, uint128 i) external view returns(uint256){
       (uint256 dy,) = _calc_withdraw_one_coin(_token_amount, i);
        return dy;
    }

    function remove_liquidity_one_coin(uint256 _token_amount, uint128 i, uint256 min_amount) external nonReentrant{
        //    Remove _amount of liquidity all in a form of coin i
        require(!is_killed,"is killed");
        uint256 dy = 0;
        uint256 dy_fee = 0;
        (dy,dy_fee) = _calc_withdraw_one_coin(_token_amount, i);
        require(dy >= min_amount, "Not enough coins removed");
        balances[i] = balances[i].sub(dy.add(dy_fee.mul(admin_fee).div(FEE_DENOMINATOR)));
        token.burnFrom(msg.sender, _token_amount);
        require(address(coins[i]).safeTransfer(msg.sender,dy),"failed transfer");

        emit RemoveLiquidityOne(msg.sender, _token_amount, dy);
    }

    /* Admin functions */

    // 到某个时间完全变成_future_A（缓慢变成A）
    function ramp_A(uint256 _future_A , uint256 _future_time) external {
        require(msg.sender == owner , "only owner");
        require(block.timestamp >= initial_A_time.add(MIN_RAMP_TIME));
        require(_future_time >= block.timestamp.add(MIN_RAMP_TIME), "insufficient time");

        uint256 _initial_A = _A();
        // bool a = (_future_A >= _initial_A) && (_future_A <= _initial_A * MAX_A_CHANGE);
        // bool b = ((_future_A < _initial_A) && (_future_A * MAX_A_CHANGE >= _initial_A));
        require((_future_A >= _initial_A) && (_future_A <= _initial_A.mul(MAX_A_CHANGE)) || ((_future_A < _initial_A) && (_future_A.mul(MAX_A_CHANGE) >= _initial_A)));
        initial_A = _initial_A;
        future_A = _future_A;
        initial_A_time = block.timestamp;
        future_A_time = _future_time;

        emit RampA(_initial_A, _future_A, block.timestamp, _future_time);
    }

    function stop_ramp_A() external{
        require(msg.sender == owner , "only owner");
        uint256 current_A = _A();
        initial_A = current_A;
        future_A = current_A;
        initial_A_time = block.timestamp;
        future_A_time = block.timestamp;

        emit StopRampA(current_A, block.timestamp);
    }

    function commit_new_fee(uint256 new_fee, uint256 new_admin_fee) external{
        require(msg.sender == owner , "only owner");
        require(admin_actions_deadline == 0, "active action");
        require(new_fee <= MAX_FEE, "fee exceeds maximum");
        require(new_admin_fee <= MAX_ADMIN_FEE, "admin fee exceeds maximum");

        uint256 _deadline = block.timestamp.add(ADMIN_ACTIONS_DELAY);
        admin_actions_deadline = _deadline;
        future_fee = new_fee;
        future_admin_fee = new_admin_fee;

        emit CommitNewFee(_deadline, new_fee, new_admin_fee);
    }

    function apply_new_fee() external{
        require(msg.sender == owner , "only owner");
        require(block.timestamp >= admin_actions_deadline, "insufficient time");
        require(admin_actions_deadline != 0, "no active action");

        admin_actions_deadline = 0;
        uint256 _fee = future_fee;
        uint256 _admin_fee = future_admin_fee;
        fee = _fee;
        admin_fee = _admin_fee;

        emit NewFee(_fee, _admin_fee);
    }

    function revert_new_parameters() external{
        require(msg.sender == owner , "only owner");
        admin_actions_deadline = 0;
    }

    function commit_transfer_ownership(address _owner) external{
        require(msg.sender == owner , "only owner");
        require(transfer_ownership_deadline == 0, "active transfer");

        uint256 _deadline = block.timestamp.add(ADMIN_ACTIONS_DELAY);
        transfer_ownership_deadline = _deadline;
        future_owner = _owner;

        emit CommitNewAdmin(_deadline, _owner);
    }

    function commit_fee_converter(address _fee_converter) external {
        require(msg.sender == owner, "only owner");
        fee_converter = _fee_converter;
        emit NewFeeConverter(_fee_converter);
    }

    function apply_transfer_ownership() external{
        require(msg.sender == owner , "only owner");
        require(block.timestamp >= transfer_ownership_deadline, "insufficient time");
        require(transfer_ownership_deadline != 0, "no active transfer");

        transfer_ownership_deadline = 0;
        address _owner = future_owner;
        owner = _owner;

        emit NewAdmin(_owner);
    }

    function revert_transfer_ownership() external{
        require(msg.sender == owner , "only owner");
        transfer_ownership_deadline = 0;
    }

    function admin_balances(uint256 i) external returns(uint256){
        return ITRC20(coins[i]).balanceOf(address(this)).sub(balances[i]);
    }

    function withdraw_admin_fees() external{
        //coin 0 must for reward
        //  require(msg.sender == owner , "only owner");
        //anybody can use this

        for(uint256 i = 0; i < N_COINS; i++){
            address c = coins[i];
            uint256 value = ITRC20(c).balanceOf(address(this)).sub(balances[i]);
            if(value > 0){
                require(address(c).safeTransfer(fee_converter, value), "failed transfer");
            }
            if (i != 1) {
                FeeConverter(fee_converter).convertFees(i, 1);
            }
        }
        FeeConverter(fee_converter).notify(coins[1]);
    }

    function donate_admin_fees() external{
        require(msg.sender == owner , "only owner");
        for(uint256 i = 0; i < N_COINS; i++){
            balances[i] = ITRC20(coins[i]).balanceOf(address(this));
        }
    }

    function kill_me() external{
        require(msg.sender == owner , "only owner");
        require(kill_deadline > block.timestamp, "deadline has passed");
        is_killed = true;
    }

    function unkill_me() external{
        require(msg.sender == owner , "only owner");
        is_killed = false;
    }

}