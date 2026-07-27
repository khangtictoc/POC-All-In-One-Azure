package com.example;

import com.azure.core.credential.AccessToken;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.core.credential.TokenRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Properties;

public class PostgresStudentApp {
    private static final Logger logger = LoggerFactory.getLogger(PostgresStudentApp.class);

    private static final String DEFAULT_DATABASE = "testing123";
    private static final String DEFAULT_HOST = "testing83547328.postgres.database.azure.com";
    private static final String DEFAULT_PORT = "5432";
    private static final String DEFAULT_STUDENT_ID = "S001";
    private static final String DEFAULT_STUDENT_NAME = "Alice Johnson";
    private static final String DEFAULT_STUDENT_AGE = "20";
    private static final String DEFAULT_STUDENT_MAJOR = "Computer Science";

    // Scope for Azure Database for PostgreSQL – Flexible Server
    // NOTE: must be "ossrdbms-aad", not "ossrdbms" — this was the root cause
    // of the AADSTS500011 error during earlier debugging.
    private static final String AZURE_POSTGRES_SCOPE = "https://ossrdbms-aad.database.windows.net/.default";

    // Postgres role name mapped to the managed identity's Object ID via
    // pgaadauth_create_principal_with_oid. This is what goes in the JDBC
    // "user" property — NOT the raw Object ID.
    private static final String DB_USER = "testing";

    public static void main(String[] args) {
        String database = getEnv("DATABASE_NAME", DEFAULT_DATABASE);
        String host = getEnv("DB_HOST", DEFAULT_HOST);
        String port = getEnv("DB_PORT", DEFAULT_PORT);
        String studentId = getEnv("STUDENT_ID", DEFAULT_STUDENT_ID);
        String studentName = getEnv("STUDENT_NAME", DEFAULT_STUDENT_NAME);
        int studentAge = Integer.parseInt(getEnv("STUDENT_AGE", DEFAULT_STUDENT_AGE));
        String studentMajor = getEnv("STUDENT_MAJOR", DEFAULT_STUDENT_MAJOR);

        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s?sslmode=require", host, port, database);

        try (Connection connection = createAzurePostgresConnection(jdbcUrl)) {
            logger.info("Connected to PostgreSQL database '{}'", database);

            createStudentTableIfNeeded(connection);

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            insertStudent(connection, studentId, studentName, studentAge, studentMajor, now);

            logger.info("Student {} inserted successfully with timestamp {}", studentId, now);
        } catch (SQLException e) {
            logger.error("Failed to connect/insert: {}", e.getMessage(), e);
        }
    }

    private static void createStudentTableIfNeeded(Connection connection) throws SQLException {
        String sql = """
            INSERT INTO students (id, name, age, major, created_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                age = EXCLUDED.age,
                major = EXCLUDED.major,
                created_at = EXCLUDED.created_at
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            logger.info("Ensured 'students' table exists");
        }
    }

    private static void insertStudent(Connection connection, String id, String name, int age, String major, OffsetDateTime timestamp) throws SQLException {
        String sql = "INSERT INTO students (id, name, age, major, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setInt(3, age);
            ps.setString(4, major);
            ps.setObject(5, timestamp); // automatically maps to TIMESTAMPTZ
            ps.executeUpdate();
        }
    }

    private static Connection createAzurePostgresConnection(String jdbcUrl) throws SQLException {
        // DefaultAzureCredential tries a chain of credential types in order
        // (Environment, WorkloadIdentity, ManagedIdentity, SharedTokenCache,
        // IntelliJ, AzureCli, AzurePowerShell, AzureDeveloperCli).
        // In a Workload-Identity-enabled AKS pod, WorkloadIdentityCredential
        // succeeds and the chain stops there.
        DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();

        TokenRequestContext request = new TokenRequestContext().addScopes(AZURE_POSTGRES_SCOPE);
        AccessToken token = credential.getToken(request).block();
        if (token == null) {
            throw new SQLException("Failed to obtain Azure AD token");
        }

        logger.info("Obtained Azure AD token for scope {}", AZURE_POSTGRES_SCOPE);

        Properties props = new Properties();
        // Postgres role name (mapped to managed identity OID via pgaadauth), not the raw OID
        props.setProperty("user", DB_USER);
        // Token becomes the password
        props.setProperty("password", token.getToken());
        props.setProperty("sslmode", "require");

        return DriverManager.getConnection(jdbcUrl, props);
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}