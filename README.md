# Money Transfer Service

A RESTful API for transferring money between accounts, built with modern Java and Spring Boot.

## Environment

- Java version: 25
- Maven version: 3.*
- Spring Boot version: 3.4.5
- Database: H2 (in-memory)
- API Documentation: SpringDoc OpenAPI (Swagger)

## Requirements

Design and implement a REST API for transferring money between accounts.

1. Coded in Java
2. Keep it very simple and to the point (e.g. no need to implement any authentication).
3. You can use any frameworks/libraries you like but be sure to keep it simple.
4. The minimal datastore should run in-memory.
5. The final result should be executable as a standalone program (should not require a pre-installed container/server).
6. Demonstrate with tests that the API works as expected.
7. Upload the exercise to GitHub.

## API Endpoints

### Account Management

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| POST | `/mybank/v1/account` | Create a new account | 201 Created |
| GET | `/mybank/v1/account/{accountId}/info` | Get account details | 200 OK / 404 Not Found |
| GET | `/mybank/v1/account/{accountId}/balance` | Get account balance | 200 OK / 400 Bad Request |

### Fund Transfer

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| POST | `/mybank/v1/transaction` | Transfer money between accounts | 202 Accepted / 400 / 404 |

### Audit Logs

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/mybank/v1/audit/account/logs` | All account audit logs | 200 OK |
| GET | `/mybank/v1/audit/account/{accountId}/logs` | Audit logs for a specific account | 200 OK / 400 Bad Request |
| GET | `/mybank/v1/audit/transaction/logs` | All fund transfer logs | 200 OK |

### Swagger UI

Once the application is running, interactive API documentation is available at:

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON specification:

```
http://localhost:8080/v3/api-docs
```

## Design

Refer documents from docs folder:
- High Level Architect Diagram
- Use Cases / Activity Diagram
- Sequence diagram

### (1) Feature Expectations

- **Use cases:** Account Creation, Balance Inquiry, Money Transfer, Transaction Logging, Audit Trail
- **Not covered:** Security, Authentication
- **Consumers:** UI calls, other services within bank

### (2) Estimations

- **Write throughput:** Account Creation (100/sec), Fund Transfer (1000/sec)
- **Read throughput:** Balance (4000/sec)
- **Read/Write ratio:** 4:1
- **Latency:** Read 200ms, Write 400ms
- **Storage (5 year):** ~10-15 GB (Account: 75 bytes x 1M/year, FundTransferLog: 100 bytes x 20M/year)
- **Memory:** 8-16 GB RAM per instance, no caching required (high consistency requirement)

Data consistency is critical for money transfers — ACID properties are enforced using `@Transactional` with `PESSIMISTIC_WRITE` locks. Accounts are locked in deterministic sorted order to prevent deadlocks during concurrent transfers.

### (3) Design Goals

- **Consistency:** Transactional writes with pessimistic locking; audit logs saved within the same transaction boundary
- **Availability:** Horizontal scaling via multiple instances; active-active or active-passive DB depending on RTO/RPO
- **Scalability:** Each API can be scaled horizontally

### (4) High Level Design

- **AccountManagement API:** Create Account, Get Balance
- **MoneyTransaction API:** Transfer money between accounts
- **AuditLog API:** Account creation audit, fund transfer audit
- **Database:** Any RDBMS (H2 in-memory for demo). Tables: `ACCOUNT`, `FUND_TRANSFER_LOG`, `ACCOUNT_AUDIT_LOG`


## Commands

### Run

```bash
mvn clean package -DskipTests
java -jar target/money-transfer-service-1.0.0.jar
```

### Install

```bash
mvn clean install
```

### Test

```bash
mvn clean test
```

## Test Structure

19 BDD-style tests organized as a sequential business flow:

```
1. Account Creation        (4 tests) — valid account, null balance default, invalid ID, negative balance
2. Account Retrieval       (2 tests) — existing account, non-existent account
3. Unsupported Operations  (1 test)  — PATCH/DELETE rejected
4. Fund Transfer           (8 tests) — service-layer transfer, API transfer, same-account, insufficient balance,
                                        overdraft, zero amount, missing account, same-account via API
5. Account Audit Logs      (3 tests) — empty logs, after creation, filtered by account ID
6. Transaction Audit Logs  (1 test)  — after successful transfer
```

## Sample Data

### Account

```json
{
    "accountId": "342233241",
    "type": "C",
    "status": "A",
    "balance": 15500
}
```

### Fund Transfer Request

```json
{
    "requestId": "REQ0001",
    "accountFromId": "342233241",
    "accountToId": "342233242",
    "amount": "1000"
}
```

### Fund Transfer Response

```json
{
    "transactionId": "00233457-609c-444a-be62-1e6406eb87c2",
    "accountFromId": "342233241",
    "amountTransfer": 1000,
    "balanceAfterTransfer": 14500.00,
    "status": "SUCCESS"
}
```

### Fund Transfer Log

```json
{
    "id": 1,
    "requestId": "REQ0001",
    "transactionId": "12af7cc1-a816-4582-b080-4bcdfde5b79d",
    "accountFromId": "342233241",
    "accountToId": "342233242",
    "amountTransfer": 1000.00,
    "status": "SUCCESS",
    "transactionDateTime": "2026-04-25T05:52:19.291+00:00",
    "comment": null
}
```

## Sample curl Commands

**Create Accounts**

```bash
curl -X POST http://localhost:8080/mybank/v1/account \
  -H 'Content-Type: application/json' \
  -d '{"accountId":"342233241","type":"C","status":"A","balance":15500}'

curl -X POST http://localhost:8080/mybank/v1/account \
  -H 'Content-Type: application/json' \
  -d '{"accountId":"342233242","type":"C","status":"A","balance":15000}'
```

**Check Balance**

```bash
curl http://localhost:8080/mybank/v1/account/342233241/balance
```

**Transfer Funds**

```bash
curl -X POST http://localhost:8080/mybank/v1/transaction \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"REQ0001","accountFromId":"342233241","accountToId":"342233242","amount":"1000"}'
```

**View Audit Logs**

```bash
curl http://localhost:8080/mybank/v1/audit/account/logs
curl http://localhost:8080/mybank/v1/audit/account/342233241/logs
curl http://localhost:8080/mybank/v1/audit/transaction/logs
```
