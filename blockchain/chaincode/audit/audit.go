package main

import (
	"encoding/json"
	"fmt"
	"regexp"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

// SmartContract provides functions for managing an AuditLog
type SmartContract struct {
	contractapi.Contract
}

// AuditLog describes basic details of what makes up a log
type AuditLog struct {
	ID          string `json:"id"`
	Timestamp   string `json:"timestamp"`
	IP          string `json:"ip"`
	User        string `json:"user"`
	PWD         string `json:"pwd"`
	Command     string `json:"command"`
	IsSensitive bool   `json:"sensitive"`
	Hash        string `json:"hash"` // Simple hash of the content
}

// InitLedger adds a base set of logs to the ledger
func (s *SmartContract) InitLedger(ctx contractapi.TransactionContextInterface) error {
	return nil
}

// CreateLog adds a new log to the world state with given details
func (s *SmartContract) CreateLog(ctx contractapi.TransactionContextInterface, id string, timestamp string, ip string, user string, pwd string, command string, hash string) (*AuditLog, error) {
	exists, err := s.LogExists(ctx, id)
	if err != nil {
		return nil, err
	}
	if exists {
		return nil, fmt.Errorf("the log %s already exists", id)
	}

	isSensitive := checkSensitive(command)

	log := AuditLog{
		ID:          id,
		Timestamp:   timestamp,
		IP:          ip,
		User:        user,
		PWD:         pwd,
		Command:     command,
		IsSensitive: isSensitive,
		Hash:        hash,
	}

	logJSON, err := json.Marshal(log)
	if err != nil {
		return nil, err
	}

	err = ctx.GetStub().PutState(id, logJSON)
	if err != nil {
		return nil, err
	}

	return &log, nil
}

// ReadLog returns the log stored in the world state with given id
func (s *SmartContract) ReadLog(ctx contractapi.TransactionContextInterface, id string) (*AuditLog, error) {
	logJSON, err := ctx.GetStub().GetState(id)
	if err != nil {
		return nil, fmt.Errorf("failed to read from world state: %v", err)
	}
	if logJSON == nil {
		return nil, fmt.Errorf("the log %s does not exist", id)
	}

	var log AuditLog
	err = json.Unmarshal(logJSON, &log)
	if err != nil {
		return nil, err
	}

	return &log, nil
}

// LogExists returns true when log with given ID exists in world state
func (s *SmartContract) LogExists(ctx contractapi.TransactionContextInterface, id string) (bool, error) {
	logJSON, err := ctx.GetStub().GetState(id)
	if err != nil {
		return false, fmt.Errorf("failed to read from world state: %v", err)
	}

	return logJSON != nil, nil
}

// GetAllLogs returns all logs found in world state
func (s *SmartContract) GetAllLogs(ctx contractapi.TransactionContextInterface) ([]*AuditLog, error) {
	// range query with empty string for startKey and endKey does an open-ended query of all assets in the chaincode namespace.
	resultsIterator, err := ctx.GetStub().GetStateByRange("", "")
	if err != nil {
		return nil, err
	}
	defer resultsIterator.Close()

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

// checkSensitive checks if the command contains sensitive operations
func checkSensitive(command string) bool {
	// Simple regex for sensitive commands
	// e.g. rm, sudo, chmod, chown, cat /etc/shadow, etc.
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

	for _, pattern := range sensitivePatterns {
		matched, _ := regexp.MatchString(pattern, command)
		if matched {
			return true
		}
	}
	return false
}

func main() {
	chaincode, err := contractapi.NewChaincode(&SmartContract{})
	if err != nil {
		fmt.Printf("Error creating audit chaincode: %s", err.Error())
		return
	}

	if err := chaincode.Start(); err != nil {
		fmt.Printf("Error starting audit chaincode: %s", err.Error())
	}
}
