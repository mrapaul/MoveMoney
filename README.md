# Transfer Service API

## Overview
This API allows users to perform internal account-to-account transfers and external transfers to a withdrawal address. It also provides endpoints to create accounts and check account balances. The service supports concurrent requests and ensures transaction integrity.

## Endpoints

### 1. Create Account
- **Endpoint**: `/create-account`
- **Method**: POST
- **Description**: Creates a new account with an initial balance.

#### Request
```json
{
  "accountId": "string",
  "userId": "string",
  "initialBalance": "number"
}
```
### Response
```json
{
  "status": "SUCCESS",
  "message": "Account created successfully"
}
```
### 2. Transfer
- **Endpoint**: /transfer
- **Method**: POST
- **Description**: Transfers an amount from one account to another account. 
### Request
```json
{
  "fromAccountId": "string",
  "toAccountId": "string",
  "amount": "number"
}
```
### Response
```json
{
  "status": "SUCCESS | FAILURE",
  "message": "string",
  "taskId": "string"
}
```
### 3. External Transfer
- **Endpoint**: /external-transfer
- **Method**: POST
- **Description**: Transfers an amount from an account to an external withdrawal address.
### Request
```json
{
  "fromAccountId": "string",
  "externalAddress": "string",
  "amount": "number"
}
```
### Response
```json
{
  "status": "SUCCESS | FAILURE",
  "message": "string",
  "taskId": "string"
}
```
### 4. Check Balance
- **Endpoint** : /balance
- **Method**: GET
- **Description**: Checks the balance of a specific account.
### Request
```json
{
  "accountId": "string"
}
```
### Response
```json
{
  "status": "SUCCESS | FAILURE",
  "balance": "number",
  "message": "string"
}
```
### Examples
### 1. Create Account
### Request
```
POST /create-account
{
  "accountId": "acc123",
  "userId": "user123",
  "initialBalance": 1000.0
}
```
### Response
```
{
  "status": "SUCCESS",
  "message": "Account created successfully"
}
```

### 2. Transfer
### Request
```
POST /transfer
{
  "fromAccountId": "acc123",
  "toAccountId": "acc456",
  "amount": 100.0
}
```
### Response
```
{
  "status": "SUCCESS",
  "message": "Transfer successful",
  "taskId": "task123"
}
```
### 3. External Transfer
### Request
```
POST /external-transfer
{
  "fromAccountId": "acc123",
  "externalAddress": "addr789",
  "amount": 100.0
}
```
### Response
```
{
  "status": "SUCCESS",
  "message": "Transfer successful",
  "taskId": "task124"
}
```
### 4. Check Balance
### Request
```
GET /balance
{
  "accountId": "acc123"
}
```
### Response
```
{
  "status": "SUCCESS",
  "balance": 900.0
}
```

### How to Run the Project
Clone the Repository:

```bash
git clone https://github.com/mrapaul/MoveMoney.git
cd transfer-service
```

### Build the Project:

```bash
./gradlew build
```

### Run the Project:

```bash
./gradlew run
```

### Access the API:
The service runs on port 8888 by default. You can access the API at http://localhost:8888.


