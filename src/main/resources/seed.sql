DO $$
DECLARE
    i INT;
BEGIN
    FOR i IN 1..3000000 LOOP
        INSERT INTO person (id, first_name, last_name, email, age, address, job_title, created_at, updated_at)
        VALUES (
            i,
            'FirstName' || i,
            'LastName' || i,
            'email' || i || '@example.com',
            (i % 50) + 20,
            'Address ' || i,
            'Job Title ' || i,
            NOW(),
            NOW()
        );
    END LOOP;
END;
$$;
