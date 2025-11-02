CREATE TABLE person (
    id BIGINT NOT NULL PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255),
    age INT,
    address VARCHAR(255),
    job_title VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
