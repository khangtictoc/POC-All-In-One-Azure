# Multi-stage Dockerfile for Azure Workload Identity Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml
COPY pom.xml .

# Download dependencies
RUN mvn dependency:resolve

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/java-postgres-wi-app.jar ./app.jar

# Set environment for Azure Identity to find workload identity token
# These will be provided by Kubernetes service account projection
ENV AZURE_FEDERATED_TOKEN_FILE="/var/run/secrets/azure/tokens/azure-identity-token"

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
