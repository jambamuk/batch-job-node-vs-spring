-----

# Spring Batch vs. Node.js Batch Performance Comparison

This project is a performance benchmark comparing a multi-threaded Spring Batch application against an asynchronous Node.js script for a large data-processing job.

The test processes **1 million records** from a PostgreSQL database, which is also containerized.

## 🚀 Overview

The goal is to compare the performance of Spring Batch and Node.js under two different scenarios:

1.  **I/O-Only Test:** How fast can each platform read 1 million records from the database? (No processing)
2.  **CPU-Bound Test:** How fast can each platform read 1 million records *and* perform a non-trivial, CPU-intensive calculation for each record?

## 📋 Requirements

  * [Docker](https://www.docker.com/)
  * [Docker Compose](https://docs.docker.com/compose/)

## ⚡ How to Run

The entire stack (PostgreSQL database, Node.js app, Spring Batch app) is managed by Docker Compose. The database will be automatically initialized with 1 million records on the first run.

1.  **Build the containers:**

    ```sh
    docker compose build
    ```

2.  **Run the entire stack:**

    ```sh
    docker compose up
    ```

This command will:

  * Start the `db` (PostgreSQL) service.
  * Run the `db-init` service to create the schema and populate 1 million `person` records (this may take a minute on the first launch).
  * Run the `nodejs-batch` job.
  * Run the `spring-batch` job.

You can observe the console output for each service to see its start time, finish time, and total records processed.

## 📈 Test Results

These benchmarks were run on a system processing 1 million records.

| Test Scenario | 🌱 Spring Batch (Java 21) | 🐢 Node.js (Node 20) |
| :--- | :--- | :--- |
| **Test 1: I/O-Only (Read-Only)** | \~8 seconds | **\~5 seconds** |
| **Test 2: CPU-Bound (Read + Process)**| **\~9 seconds** | \~17 seconds |
| **Memory Usage (Observed)** | High | Low |

-----

## 🧠 Analysis & Conclusion

The results clearly show the core trade-off between the two stacks.

### Test 1: I/O-Only (Read-Only)

**Winner: Node.js**

  * Node.js was faster because of its lightweight, non-blocking I/O model and extremely fast startup time.
  * Spring Batch was slower due to the fixed overhead of JVM startup, Spring Framework initialization, and the transactional/metadata management it performs for each chunk.

### Test 2: CPU-Bound (Read + Process)

**Winner: Spring Batch**

  * **Spring Batch**'s time barely increased (from 8s to 9s). Its `taskExecutor` (configured with 10 workers) ran the CPU-intensive work in **true parallel** across multiple threads/cores.
  * **Node.js**'s time exploded (from 5s to 17s). Because Node.js is **single-threaded**, the "parallel" `Promise.all` was forced to run the CPU-bound tasks **sequentially**, one after another, as each one blocked the event loop.
  * **Memory Trade-off:** The speed of Spring Batch comes at the cost of higher memory usage (JVM heap, holding multiple chunks in memory for parallel workers). Node.js, while slow, remained very memory-efficient.

### Final Verdict

  * For **I/O-bound** batch jobs (like simple data migration), **Node.js** is a highly efficient and lightweight choice.
  * For **CPU-bound** batch jobs (like complex transformations, calculations, or parsing), **Spring Batch** is overwhelmingly faster, as its multi-threaded model can leverage all available CPU cores.
