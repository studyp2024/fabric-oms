package main

import (
	"encoding/json"
	"fmt"
	"regexp"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

// SmartContract 提供管理审计日志的智能合约功能
type SmartContract struct {
	contractapi.Contract
}

// AuditLog 定义了组成一条审计日志的基本属性
type AuditLog struct {
	ID          string `json:"id"`        // 日志唯一标识（通常是哈希值）
	Timestamp   string `json:"timestamp"` // 命令执行的时间戳
	IP          string `json:"ip"`        // 发起操作的客户端 IP
	ServerIP    string `json:"serverIp"`  // 目标服务器 IP
	User        string `json:"user"`      // 执行命令的 SSH 用户名
	PWD         string `json:"pwd"`       // 执行命令时的当前工作目录
	Command     string `json:"command"`   // 执行的具体命令
	IsSensitive bool   `json:"sensitive"` // 是否被判定为高危/敏感命令
	Hash        string `json:"hash"`      // 原始日志内容的简单哈希（防篡改校验）
}

// InitLedger 初始化账本，通常用于设置初始状态（当前为空实现）
func (s *SmartContract) InitLedger(ctx contractapi.TransactionContextInterface) error {
	return nil
}

// CreateLog 接收日志详情，并在世界状态中创建一条新日志记录
func (s *SmartContract) CreateLog(ctx contractapi.TransactionContextInterface, id string, timestamp string, ip string, serverIp string, user string, pwd string, command string, hash string) (*AuditLog, error) {
	// 1. 检查日志是否已经存在，防止重复上链
	exists, err := s.LogExists(ctx, id)
	if err != nil {
		return nil, err
	}
	if exists {
		return nil, fmt.Errorf("the log %s already exists", id)
	}

	// 2. 在链码层自动检测该命令是否属于敏感操作
	isSensitive := checkSensitive(command)

	// 3. 构建日志对象
	log := AuditLog{
		ID:          id,
		Timestamp:   timestamp,
		IP:          ip,
		ServerIP:    serverIp,
		User:        user,
		PWD:         pwd,
		Command:     command,
		IsSensitive: isSensitive,
		Hash:        hash,
	}

	// 4. 将日志对象序列化为 JSON 格式
	logJSON, err := json.Marshal(log)
	if err != nil {
		return nil, err
	}

	// 5. 调用 PutState 将数据写入 Fabric 账本的世界状态
	err = ctx.GetStub().PutState(id, logJSON)
	if err != nil {
		return nil, err
	}

	return &log, nil
}

// ReadLog 根据给定的 id，从世界状态中读取对应的日志信息
func (s *SmartContract) ReadLog(ctx contractapi.TransactionContextInterface, id string) (*AuditLog, error) {
	// 调用 GetState 从账本中查询数据
	logJSON, err := ctx.GetStub().GetState(id)
	if err != nil {
		return nil, fmt.Errorf("failed to read from world state: %v", err)
	}
	if logJSON == nil {
		return nil, fmt.Errorf("the log %s does not exist", id)
	}

	var log AuditLog
	// 将 JSON 数据反序列化为对象
	err = json.Unmarshal(logJSON, &log)
	if err != nil {
		return nil, err
	}

	return &log, nil
}

// LogExists 检查给定 ID 的日志在世界状态中是否存在，存在返回 true
func (s *SmartContract) LogExists(ctx contractapi.TransactionContextInterface, id string) (bool, error) {
	logJSON, err := ctx.GetStub().GetState(id)
	if err != nil {
		return false, fmt.Errorf("failed to read from world state: %v", err)
	}

	return logJSON != nil, nil
}

// GetAllLogs 返回世界状态中找到的所有日志记录
func (s *SmartContract) GetAllLogs(ctx contractapi.TransactionContextInterface) ([]*AuditLog, error) {
	// 使用空字符串作为 startKey 和 endKey，可以查询链码命名空间下的所有资产
	resultsIterator, err := ctx.GetStub().GetStateByRange("", "")
	if err != nil {
		return nil, err
	}
	defer resultsIterator.Close() // 必须确保迭代器被关闭

	var logs []*AuditLog
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return nil, err
		}

		var log AuditLog
		err = json.Unmarshal(queryResponse.Value, &log)
		if err != nil {
			return nil, err
		}
		logs = append(logs, &log)
	}

	return logs, nil
}

// checkSensitive 检查传入的命令是否包含敏感或高危操作
func checkSensitive(command string) bool {
	// 定义敏感命令的正则表达式列表
	// 例如：rm（删除）, sudo（提权）, chmod/chown（改权限）, cat /etc/shadow（看密码文件）等
	sensitivePatterns := []string{
		`\brm\b`,
		`\bsudo\b`,
		`\bchmod\b`,
		`\bchown\b`,
		`\bpasswd\b`,
		`\bshadow\b`,
		`\buseradd\b`,
		`\buserdel\b`,
		`\bgroupadd\b`,
		`\bgroupdel\b`,
		`\binit\b\s+[0-6]`,
		`\breboot\b`,
		`\bshutdown\b`,
	}

	// 遍历正则列表，一旦匹配上就判定为敏感操作
	for _, pattern := range sensitivePatterns {
		matched, _ := regexp.MatchString(pattern, command)
		if matched {
			return true
		}
	}
	return false
}

func main() {
	// 创建新的链码实例
	chaincode, err := contractapi.NewChaincode(&SmartContract{})
	if err != nil {
		fmt.Printf("Error creating audit chaincode: %s", err.Error())
		return
	}

	// 启动链码服务
	if err := chaincode.Start(); err != nil {
		fmt.Printf("Error starting audit chaincode: %s", err.Error())
	}
}
