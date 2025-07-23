# 💸 Transaction Processing System

A multi-threaded financial transaction processor that produces, tracks, audits, and exposes account balance via a secure REST API.

---

## 🔐 Authentication

All secured endpoints require **Basic Authentication**.

- **Username:** `test`
- **Password:** `P@ssword12`

---

## 🔗 REST API

### `GET /api/v1/balance`

Returns the current account balance.

#### ✔️ 200 OK

HTTP/1.1 200 OK
Content-Type: application/json

{
    "availableBalance": "568058.85"
}

#### ❌ 401 Unauthorized

HTTP/1.1 401 Unauthorized
{
"error": "Unauthorized access"
}

shell
Copy
Edit

#### ❌ 500 Internal Server Error

HTTP/1.1 500 Internal Server Error
{
"error": "Service currently unavailable"
}

---

## ⚙️ Components Overview

### 🏭 Producer

- Automatically starts on app load.
- Spawns two dedicated threads:
  - One for **credit** transactions
  - One for **debit** transactions
- Randomly generates transactions every 40ms using injected `Random` bean or `Supplier<Double>`.

### 📈 Tracker

- Thread-safe `BalanceTracker` processes each transaction.
- Maintains the current balance using atomic data structures.
- Exposes `getBalance()` for controller usage.

### 📦 Audit Service

Consists of two flows:

1. **Ingestion Flow**
   - Accepts transactions non-blockingly into a queue.
2. **Submission Flow**
   - Scheduler kicks off submission process.
   - Forms optimized batches using scoring-based bin packing.

---

## 📊 Batch Optimization Comparison

| Strategy        | 1,000 Txns | Avg Batch Size | 100,000 Txns | Avg Batch Size |
|-----------------|------------|----------------|--------------|----------------|
| Best Fit        | ✅         | 250            | ✅           | 250,000        |
| First Fit       | ✅         | 245            | ✅           | 250,000        |
| **Scoring-Based** | ✅       | 240            | ✅           | **250,000**    |

> 🔍 **Scoring-Based** was selected for production due to lower memory and CPU overheads, despite slightly smaller batch size in low-volume cases.

---

## 🚀 How to Run

### Build


- mvn clean install

### Run

- java -Dspring.profiles.active=prod -jar target/transaction-processor.jar
### Testing
Run unit and integration tests:


mvn test
- Coverage includes:

- Balance retrieval

- Producer behavior

- Transaction batching

- Audit submission scheduling


