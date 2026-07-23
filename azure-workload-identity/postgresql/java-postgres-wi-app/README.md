# Java PostgreSQL Flexible Server App

This sample app connects to an Azure PostgreSQL Flexible Server, ensures a `student` table exists, and inserts one student record.

## Required database information

- PostgreSQL server name: `testing83547328`
- Database name: `testing123`
- Table name: `student`

## SQL commands

Create the database and table with the `created_at` timestamp column:

```sql
CREATE DATABASE testing123;
\c testing123

CREATE TABLE student (
  student_id VARCHAR(64) PRIMARY KEY,
  student_name TEXT NOT NULL,
  student_age INT,
  student_major TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO student (student_id, student_name, student_age, student_major)
VALUES ('S001', 'Alice Johnson', 20, 'Computer Science');
```

If you wish to explicitly insert `created_at` manually:

```sql
INSERT INTO student (student_id, student_name, student_age, student_major, created_at)
VALUES ('S001', 'Alice Johnson', 20, 'Computer Science', CURRENT_TIMESTAMP);
```

> If you connect with the Azure PostgreSQL admin user, you may first connect to the default `postgres` database and then run `CREATE DATABASE testing123;`.

## Build and run

```bash
cd '/home/virus/Personal Project/x_temp/java-postgres-wi-app'
./mvnw clean package
java -jar target/java-postgres-wi-app.jar
```

## Environment variables

Set the following environment variables before running. Replace the placeholder values with your actual Azure PostgreSQL credentials.

```bash
export POSTGRES_HOST=testing83547328.postgres.database.azure.com
export POSTGRES_PORT=5432
export POSTGRES_DATABASE=testing123
export POSTGRES_USER=<your-user>@testing83547328
export POSTGRES_PASSWORD=<your-password>
export STUDENT_ID=S001
export STUDENT_NAME="Alice Johnson"
export STUDENT_AGE=20
export STUDENT_MAJOR="Computer Science"
```

## Notes

- The app uses SSL mode `require` because Azure PostgreSQL Flexible Server requires encrypted connections.
- If the `student` record already exists, the app updates the row using the same `student_id`.
