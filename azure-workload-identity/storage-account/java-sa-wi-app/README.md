# Azure Workload Identity Service

A Java service that uses **Azure Workload Identity** to authenticate with Azure Storage and upload timestamped files.

## Overview

This service:

- ✅ Uses `DefaultAzureCredential` for seamless authentication
- ✅ Authenticates via Kubernetes Workload Identity (no secrets stored)
- ✅ Creates a file named with current date/time (format: `yyyy-MM-dd_HH-mm-ss`)
- ✅ Uploads the empty file to Azure Storage Blob Container
- ✅ Includes comprehensive logging for troubleshooting

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Docker (for building the container image)
- Azure subscription with:
  - Storage Account: `testing1231254984`
  - Blob Container: `testing`
  - AKS cluster with Workload Identity enabled
  - Service Account with proper annotations and labels

## Building Locally

### 1. Build with Maven

```bash
mvn clean package
```

This creates:

- `target/app-uber.jar` - Uber JAR with all dependencies

### 2. Run Locally (with Service Principal or User-Assigned Managed Identity)

Set up Azure authentication:

```bash
# Option A: Use Azure CLI authentication
az login

# Option B: Use environment variables for Service Principal
export AZURE_CLIENT_ID=<client-id>
export AZURE_CLIENT_SECRET=<client-secret>
export AZURE_TENANT_ID=<tenant-id>
```

Then run:

```bash
java -jar target/app-uber.jar
```

## Docker Build and Push

### 1. Build the Docker Image

```bash
docker build -t testing1231254984.azurecr.io/azure-workload-identity-app:v1.0.0 .
```

### 2. Push to Azure Container Registry

```bash
# Login to ACR
az acr login --name testing1231254984

# Push image
docker push testing1231254984.azurecr.io/azure-workload-identity-app:v1.0.0
```

## Kubernetes Deployment with Workload Identity

### 1. Prepare Service Account

Create a service account with Workload Identity annotations:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: azure-storage-sa
  namespace: default
  annotations:
    azure.workload.identity/client-id: <USER_ASSIGNED_IDENTITY_CLIENT_ID>
    azure.workload.identity/tenant-id: <TENANT_ID>
  labels:
    azure.workload.identity/use: "true"
```

### 2. Create Pod with Workload Identity

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: storage-upload-pod
  namespace: default
  labels:
    azure.workload.identity/use: "true"
spec:
  serviceAccountName: azure-storage-sa
  containers:
    - name: app
      image: testing1231254984.azurecr.io/azure-workload-identity-app:v1.0.0
      imagePullPolicy: Always
      env:
        - name: AZURE_FEDERATED_TOKEN_FILE
          value: /var/run/secrets/azure/tokens/azure-identity-token
        - name: AZURE_CLIENT_ID
          valueFrom:
            fieldRef:
              fieldPath: metadata.annotations['azure.workload.identity/client-id']
        - name: AZURE_TENANT_ID
          valueFrom:
            fieldRef:
              fieldPath: metadata.annotations['azure.workload.identity/tenant-id']
      volumeMounts:
        - name: azure-identity-token
          mountPath: /var/run/secrets/azure/tokens
          readOnly: true
  volumes:
    - name: azure-identity-token
      projected:
        sources:
          - serviceAccountToken:
              audience: api://AzureADTokenExchange
              expirationSeconds: 3600
              path: azure-identity-token
```

### 3. Required Azure Resources

**User-Assigned Managed Identity:**

```bash
# Create managed identity
az identity create \
  --resource-group <RG_NAME> \
  --name azure-storage-identity

# Get values for Kubernetes annotations
az identity show \
  --resource-group <RG_NAME> \
  --name azure-storage-identity \
  --query clientId -o tsv

az account show --query tenantId -o tsv
```

**Storage Account Permissions:**

```bash
# Grant Storage Blob Data Contributor role
az role assignment create \
  --role "Storage Blob Data Contributor" \
  --assignee <CLIENT_ID> \
  --scope /subscriptions/<SUBSCRIPTION>/resourceGroups/<RG>/providers/Microsoft.Storage/storageAccounts/testing1231254984
```

**Federate the Identity:**

```bash
az identity federated-credential create \
  --resource-group <RG_NAME> \
  --identity-name azure-storage-identity \
  --name azure-fed-cred \
  --issuer https://oidcToken.blob.core.windows.net/<SUBSCRIPTION_ID>/discovery/v1.0/keys \
  --subject system:serviceaccount:default:azure-storage-sa \
  --audiences api://AzureADTokenExchange
```

## Environment Variables

The service respects these environment variables set by Kubernetes:

| Variable                     | Description                   | Set By         |
| ---------------------------- | ----------------------------- | -------------- |
| `AZURE_FEDERATED_TOKEN_FILE` | Path to federated token       | Pod spec       |
| `AZURE_CLIENT_ID`            | Service principal/identity ID | Pod annotation |
| `AZURE_TENANT_ID`            | Azure tenant ID               | Pod annotation |

## Project Structure

```
.
├── pom.xml
├── Dockerfile
├── src/
│   └── main/
│       ├── java/com/example/
│       │   └── AzureStorageUploadService.java
│       └── resources/
│           └── logback.xml
└── README.md
```

## Logging

The service uses SLF4J with Logback for structured logging. Logs are written to stdout for Kubernetes integration.

Check logs with:

```bash
kubectl logs pod/storage-upload-pod
```

## Troubleshooting

### Authentication Failures

```bash
# Check if service account has correct annotations
kubectl get serviceaccount azure-storage-sa -o yaml

# Check pod events
kubectl describe pod storage-upload-pod

# Verify federated credential
az identity federated-credential show \
  --resource-group <RG_NAME> \
  --identity-name azure-storage-identity \
  --name azure-fed-cred
```

### Permission Errors

```bash
# Verify role assignment
az role assignment list \
  --assignee <CLIENT_ID> \
  --scope /subscriptions/<SUBSCRIPTION>/resourceGroups/<RG>/providers/Microsoft.Storage/storageAccounts/testing1231254984
```

## Next Steps

1. Build the Docker image
2. Push to your container registry
3. Set up Azure resources (managed identity, role assignments, federated credentials)
4. Deploy the service account and pod to your AKS cluster
5. Monitor logs: `kubectl logs -f pod/storage-upload-pod`

## License

MIT
