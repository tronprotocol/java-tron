#!/bin/bash

# 循环运行 LiteFnQueryHttpFilterTest 和 LiteFnQueryGrpcInterceptorTest 的脚本
# 作者: 自动生成
# 日期: $(date)

set -e  # 遇到错误时退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置参数
DEFAULT_ITERATIONS=10
LOG_DIR="./lite_fn_test_logs"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$LOG_DIR/test_run_$TIMESTAMP.log"
SUMMARY_FILE="$LOG_DIR/test_summary_$TIMESTAMP.log"

# 测试类名
HTTP_TEST="org.tron.core.services.filter.LiteFnQueryHttpFilterTest"
GRPC_TEST="org.tron.core.services.filter.LiteFnQueryGrpcInterceptorTest"

# 创建日志目录
mkdir -p "$LOG_DIR"

# 帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo "选项:"
    echo "  -n, --iterations NUM    运行次数 (默认: $DEFAULT_ITERATIONS)"
    echo "  -h, --help             显示此帮助信息"
    echo "  -v, --verbose          详细输出模式"
    echo "  --http-only            只运行 HTTP 测试"
    echo "  --grpc-only            只运行 gRPC 测试"
    echo ""
    echo "示例:"
    echo "  $0 -n 20              # 运行 20 次"
    echo "  $0 --http-only -n 5    # 只运行 HTTP 测试 5 次"
    echo "  $0 --grpc-only         # 只运行 gRPC 测试 10 次"
}

# 解析命令行参数
ITERATIONS=$DEFAULT_ITERATIONS
VERBOSE=false
HTTP_ONLY=false
GRPC_ONLY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -n|--iterations)
            ITERATIONS="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        --http-only)
            HTTP_ONLY=true
            shift
            ;;
        --grpc-only)
            GRPC_ONLY=true
            shift
            ;;
        *)
            echo "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 验证参数
if ! [[ "$ITERATIONS" =~ ^[0-9]+$ ]] || [ "$ITERATIONS" -le 0 ]; then
    echo -e "${RED}错误: 运行次数必须是正整数${NC}"
    exit 1
fi

if [ "$HTTP_ONLY" = true ] && [ "$GRPC_ONLY" = true ]; then
    echo -e "${RED}错误: --http-only 和 --grpc-only 不能同时使用${NC}"
    exit 1
fi

# 统计变量
HTTP_SUCCESS=0
HTTP_FAILED=0
GRPC_SUCCESS=0
GRPC_FAILED=0
TOTAL_START_TIME=$(date +%s)

# 日志函数
log_info() {
    local message="$1"
    echo -e "${BLUE}[INFO]${NC} $message" | tee -a "$LOG_FILE"
}

log_success() {
    local message="$1"
    echo -e "${GREEN}[SUCCESS]${NC} $message" | tee -a "$LOG_FILE"
}

log_error() {
    local message="$1"
    echo -e "${RED}[ERROR]${NC} $message" | tee -a "$LOG_FILE"
}

log_warning() {
    local message="$1"
    echo -e "${YELLOW}[WARNING]${NC} $message" | tee -a "$LOG_FILE"
}

# 运行单个测试的函数
run_test() {
    local test_class="$1"
    local test_name="$2"
    local iteration="$3"
    
    log_info "第 $iteration 次运行 $test_name 测试..."
    
    # 先执行clean
    log_info "执行clean操作..."
    local clean_output=$(./gradlew clean 2>&1)
    local clean_exit_code=$?
    
    if [ $clean_exit_code -ne 0 ]; then
        log_error "Clean操作失败"
        echo "Clean错误输出:" >> "$LOG_FILE"
        echo "$clean_output" >> "$LOG_FILE"
        return 1
    fi
    
    local start_time=$(date +%s)
    local test_output
    
    if [ "$VERBOSE" = true ]; then
        test_output=$(./gradlew :framework:test --tests "$test_class" --rerun-tasks 2>&1)
    else
        test_output=$(./gradlew :framework:test --tests "$test_class" --rerun-tasks 2>&1 | grep -E "(PASSED|FAILED|BUILD|ERROR)")
    fi
    
    local exit_code=$?
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo "$test_output" >> "$LOG_FILE"
    
    if [ $exit_code -eq 0 ]; then
        log_success "$test_name 测试通过 (耗时: ${duration}s)"
        return 0
    else
        log_error "$test_name 测试失败 (耗时: ${duration}s)"
        echo "错误输出:" >> "$LOG_FILE"
        echo "$test_output" >> "$LOG_FILE"
        return 1
    fi
}

# 主执行逻辑
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}    LiteFn 测试循环运行脚本${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
log_info "开始测试运行..."
log_info "运行次数: $ITERATIONS"
log_info "日志文件: $LOG_FILE"
log_info "汇总文件: $SUMMARY_FILE"

if [ "$HTTP_ONLY" = true ]; then
    log_info "模式: 仅运行 HTTP 测试"
elif [ "$GRPC_ONLY" = true ]; then
    log_info "模式: 仅运行 gRPC 测试"
else
    log_info "模式: 运行所有测试"
fi

echo ""

# 执行测试循环
for i in $(seq 1 $ITERATIONS); do
    echo -e "${YELLOW}======== 第 $i/$ITERATIONS 轮测试 ========${NC}"
    
    # 运行 HTTP 测试
    if [ "$GRPC_ONLY" != true ]; then
        if run_test "$HTTP_TEST" "HTTP Filter" "$i"; then
            ((HTTP_SUCCESS++))
        else
            ((HTTP_FAILED++))
        fi
    fi
    
    # 运行 gRPC 测试
    if [ "$HTTP_ONLY" != true ]; then
        if run_test "$GRPC_TEST" "gRPC Interceptor" "$i"; then
            ((GRPC_SUCCESS++))
        else
            ((GRPC_FAILED++))
        fi
    fi
    
    echo ""
done

# 计算总耗时
TOTAL_END_TIME=$(date +%s)
TOTAL_DURATION=$((TOTAL_END_TIME - TOTAL_START_TIME))

# 生成汇总报告
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}           测试汇总报告${NC}"
echo -e "${BLUE}========================================${NC}"

{
    echo "测试汇总报告"
    echo "生成时间: $(date)"
    echo "总运行时间: ${TOTAL_DURATION}s"
    echo "运行次数: $ITERATIONS"
    echo ""
    
    if [ "$GRPC_ONLY" != true ]; then
        echo "HTTP Filter 测试结果:"
        echo "  成功: $HTTP_SUCCESS"
        echo "  失败: $HTTP_FAILED"
        echo "  成功率: $(( HTTP_SUCCESS * 100 / (HTTP_SUCCESS + HTTP_FAILED) ))%"
        echo ""
    fi
    
    if [ "$HTTP_ONLY" != true ]; then
        echo "gRPC Interceptor 测试结果:"
        echo "  成功: $GRPC_SUCCESS"
        echo "  失败: $GRPC_FAILED"
        echo "  成功率: $(( GRPC_SUCCESS * 100 / (GRPC_SUCCESS + GRPC_FAILED) ))%"
        echo ""
    fi
    
    TOTAL_SUCCESS=$((HTTP_SUCCESS + GRPC_SUCCESS))
    TOTAL_FAILED=$((HTTP_FAILED + GRPC_FAILED))
    TOTAL_TESTS=$((TOTAL_SUCCESS + TOTAL_FAILED))
    
    echo "总体结果:"
    echo "  总测试数: $TOTAL_TESTS"
    echo "  总成功数: $TOTAL_SUCCESS"
    echo "  总失败数: $TOTAL_FAILED"
    if [ $TOTAL_TESTS -gt 0 ]; then
        echo "  总成功率: $(( TOTAL_SUCCESS * 100 / TOTAL_TESTS ))%"
    fi
} | tee "$SUMMARY_FILE"

echo ""
log_info "测试完成！详细日志请查看: $LOG_FILE"
log_info "汇总报告请查看: $SUMMARY_FILE"

# 根据结果设置退出码
if [ $TOTAL_FAILED -gt 0 ]; then
    log_warning "存在失败的测试，请检查日志"
    exit 1
else
    log_success "所有测试都通过了！"
    exit 0
fi
