const { Pool } = require('pg');

const DB_HOST = process.env.DB_HOST || 'db';
const DB_USER = process.env.DB_USER || 'user';
const DB_PASSWORD = process.env.DB_PASSWORD || 'password';
const DB_NAME = process.env.DB_NAME || 'batchdb';
const BATCH_SIZE = parseInt(process.env.BATCH_SIZE || '2000', 10);
const CONCURRENCY = parseInt(process.env.CONCURRENCY || '10', 10);

const pool = new Pool({
  host: DB_HOST,
  user: DB_USER,
  password: DB_PASSWORD,
  database: DB_NAME,
  port: 5432,
  // Recommended: Set pool size to match or exceed concurrency
  max: CONCURRENCY + 2,
});

const simulateProcessing = (person) => {
  return new Promise(resolve => {
    // SIMULATE CPU-BOUND WORK
    let total = 0;
    for (let i = 0; i < 5000; i++) { // A small busy-loop
      total += Math.sqrt(i);
    }
    // Add a (meaningless) property to the person
    person.processedValue = total;
    resolve(person);
  });
};
const processBatch = async (batch) => {
  const processingPromises = batch.map(simulateProcessing);
  await Promise.all(processingPromises);
  // console.log(`Processed batch of ${batch.length} items.`);
};

const runBatchJob = async () => {
  console.log('Node.js Batch Job Started (Optimized Version)');
  const startTime = Date.now();

  // Use a single client for the sequential reading phase
  const client = await pool.connect();

  let lastId = 0; // Use lastId for keyset pagination, not offset
  let totalProcessed = 0;
  let keepFetching = true;

  try {
    while (keepFetching) {
      const activeProcessingPromises = [];

      // ===== PHASE 1: READ (Sequential) =====
      // The "Reader" fetches CONCURRENCY number of chunks sequentially.
      // This is very fast because the queries are indexed and simple.
      for (let i = 0; i < CONCURRENCY; i++) {
        const res = await client.query(
          `SELECT id, first_name, last_name, email, age, address, job_title, created_at, updated_at 
           FROM person 
           WHERE id > $1 
           ORDER BY id 
           LIMIT $2`,
          [lastId, BATCH_SIZE]
        );

        const batch = res.rows;

        if (batch.length === 0) {
          keepFetching = false; // No more data, stop the outer loop
          break; // Exit this inner for-loop
        }

        totalProcessed += batch.length;
        // Get the ID of the last item in *this* batch to use for the *next* query
        lastId = batch[batch.length - 1].id;

        // Add the processing task to the list, but don't await it yet.
        activeProcessingPromises.push(processBatch(batch));
      }

      // ===== PHASE 2: PROCESS (Parallel) =====
      // Now, wait for all the chunks we just fetched to be processed concurrently.
      if (activeProcessingPromises.length > 0) {
        await Promise.all(activeProcessingPromises);
      }
    }

  } finally {
    client.release();
  }

  const endTime = Date.now();
  const duration = (endTime - startTime) / 1000;

  console.log(`Node.js Batch Job Finished. Total processed: ${totalProcessed} items in ${duration} seconds.`);
  await pool.end();
};

runBatchJob().catch(err => {
  console.error('Node.js Batch Job Failed:', err);
  process.exit(1);
});
