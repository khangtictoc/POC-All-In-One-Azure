package com.example;

import com.azure.core.util.BinaryData;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Azure Workload Identity Service
 * Creates a timestamped file and uploads it to Azure Storage Blob Container
 * Uses DefaultAzureCredential to authenticate via Workload Identity in Kubernetes
 */
public class AzureStorageUploadService {
    private static final Logger logger = LoggerFactory.getLogger(AzureStorageUploadService.class);

    // Configuration constants
    private static final String STORAGE_ACCOUNT_NAME = "testing1231254984";
    private static final String CONTAINER_NAME = "testing";
    private static final String STORAGE_ACCOUNT_URL = String.format("https://%s.blob.core.windows.net", STORAGE_ACCOUNT_NAME);

    // Date format: yyyy-MM-dd_HH-mm-ss (accurate to seconds)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static void main(String[] args) {
        logger.info("Starting Azure Storage Upload Service with Workload Identity");
        
        try {
            // Create the timestamped filename
            String fileName = generateFileName();
            logger.info("Generated filename: {}", fileName);

            // Upload file to Azure Storage
            uploadFileToAzureStorage(fileName);

            logger.info("File successfully uploaded to Azure Storage");
        } catch (Exception e) {
            logger.error("Error occurred during file upload", e);
            System.exit(1);
        }
    }

    /**
     * Generate a filename based on current date and time (accurate to seconds)
     * Format: yyyy-MM-dd_HH-mm-ss
     *
     * @return Generated filename
     */
    private static String generateFileName() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DATE_FORMATTER);
    }

    /**
     * Upload an empty file to Azure Blob Storage using Workload Identity
     * 
     * @param fileName Name of the file to create and upload
     */
    private static void uploadFileToAzureStorage(String fileName) {
        // Build DefaultAzureCredential for Workload Identity authentication
        // This uses:
        // 1. AZURE_FEDERATED_TOKEN_FILE (from projected volume)
        // 2. AZURE_CLIENT_ID (from service account annotation)
        // 3. AZURE_TENANT_ID (from service account annotation)
        logger.debug("Building DefaultAzureCredential for Workload Identity");
        DefaultAzureCredential credential = new DefaultAzureCredentialBuilder()
                .build();

        try {
            // Create BlobServiceClient
            logger.debug("Creating BlobServiceClient for: {}", STORAGE_ACCOUNT_URL);
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .endpoint(STORAGE_ACCOUNT_URL)
                    .credential(credential)
                    .buildClient();

            // Get container client
            logger.debug("Getting container client: {}", CONTAINER_NAME);
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);

            // Get blob client
            logger.debug("Getting blob client for file: {}", fileName);
            BlobClient blobClient = containerClient.getBlobClient(fileName);

            // Upload empty file
            logger.info("Uploading file '{}' to container '{}'", fileName, CONTAINER_NAME);
            BinaryData emptyData = BinaryData.fromString("");
            blobClient.upload(emptyData, true);

            logger.info("File '{}' successfully uploaded", fileName);

        } catch (Exception e) {
            logger.error("Failed to upload file '{}' to Azure Storage", fileName, e);
            throw new RuntimeException("File upload failed", e);
        }
    }
}
