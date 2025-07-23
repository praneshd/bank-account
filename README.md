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

## Assumptions
- No Overdraft Enforcement:
Account balances are allowed to go negative. There is no overdraft limit or warning. All debit transactions are processed regardless of balance.

- Transaction Amount Representation:
Amounts are stored in pence (integer values) to prevent floating-point precision errors.
Credits are positive values, and debits are negative values.
- Batching Logic:
A batch can contain up to 1000 transactions.
The total value of a batch cannot exceed £1,000,000 (i.e., 100,000,000 pence).
Transactions that would breach the batch limit are skipped with a warning log.

- Thread Safety:
The transaction queue (LinkedBlockingQueue) is thread-safe.
A Semaphore is used to limit the number of concurrent batch processing threads.
A custom ExecutorService is used to manage thread execution safely.

- Asynchronous Processing Trigger:
There is no scheduled batching.
When the queue reaches the threshold (e.g., 1000 transactions), processing is triggered immediately on a new thread (if available).

- Submission Processing Failures:
Any exceptions during submission handling are logged.
There is no automatic retry mechanism for failed submissions.

- Transaction Ordering:
Transactions are processed in FIFO order from the queue.
Due to parallel execution, batch submission order is not guaranteed.

- No Persistent Storage:
The system maintains all state in memory.
Data is not persisted across application restarts.

## Limitations
- No Retry or Dead Letter Queue:
Failed batch submissions are logged but not retried or saved for future processing.

- No Backpressure or Throttling:
The system doesn't support explicit backpressure or rate limiting for transaction ingestion.

- Non-deterministic Batch Assignment:
As batching is multi-threaded, the grouping of transactions into batches may vary per run.

- Oversized Transactions Are Ignored:
Any single transaction exceeding the batch total limit is skipped and not re-queued.

## Configuration
Thread Pool Size:
The number of audit worker threads for batch processing is configurable via application properties (e.g., audit.thread.pool.size).
This allows tuning for different load conditions or deployment environments
