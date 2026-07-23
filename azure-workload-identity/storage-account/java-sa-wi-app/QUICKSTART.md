# Project Summary - Azure Workload Identity Service

## 📦 What's Included

This is a complete, production-ready Java service for Azure Workload Identity authentication. Here's what's been created:

### Core Files

| File                                                         | Purpose                                         |
| ------------------------------------------------------------ | ----------------------------------------------- |
| **pom.xml**                                                  | Maven configuration with all Azure dependencies |
| **src/main/java/com/example/AzureStorageUploadService.java** | Main application class                          |
| **src/main/resources/logback.xml**                           | Logging configuration                           |
| **Dockerfile**                                               | Multi-stage Docker build for the application    |

### Kubernetes Files

| File                  | Purpose                                                 |
| --------------------- | ------------------------------------------------------- |
| **k8s-manifest.yaml** | Sample Service Account, Pod, and CronJob configurations |

### Helper Scripts

| File                  | Purpose                                             |
| --------------------- | --------------------------------------------------- |
| **build-and-push.sh** | Automated Docker build and push to ACR              |
| **azure-setup.sh**    | Automated Azure resource creation and configuration |

### Documentation

| File              | Purpose                             |
| ----------------- | ----------------------------------- |
| **README.md**     | Complete guide with troubleshooting |
| **QUICKSTART.md** | This file - quick reference         |

---

## 🚀 Quick Start

### 1. Build the Application

```bash
mvn clean package
```

### 2. Build and Push Docker Image

```bash
# Make script executable (one-time)
chmod +x build-and-push.sh

# Build and push to ACR
./build-and-push.sh v1.0.0
```

### 3. Set Up Azure Resources

```bash
# Make script executable (one-time)
chmod +x azure-setup.sh

# Run interactive setup
./azure-setup.sh
```

This script will:

- Create a User-Assigned Managed Identity
- Grant Storage Blob Data Contributor role
- Set up Workload Identity federation

### 4. Deploy to Kubernetes

```bash
# Update with your values
vim k8s-manifest.yaml

# Deploy
kubectl apply -f k8s-manifest.yaml

# Check logs
kubectl logs pod/storage-upload-pod
```

---

## 📋 What the Service Does

✅ **Creates a timestamped file** with format: `yyyy-MM-dd_HH-mm-ss`
✅ **Authenticates via Workload Identity** (no stored secrets!)
✅ **Uploads to Azure Storage** in container `testing`
✅ **Provides detailed logging** for troubleshooting

---

## 🔐 How Workload Identity Works

```
Kubernetes Pod
    ↓
Service Account + Federated Token
    ↓
DefaultAzureCredential
    ↓
Azure Managed Identity
    ↓
Azure Storage (with RBAC)
```

No secrets stored anywhere! Pure token-based authentication.

---

## 📝 Configuration

All configuration is in Kubernetes manifests:

```yaml
# Update these in k8s-manifest.yaml:
USER_ASSIGNED_IDENTITY_CLIENT_ID: "<your-client-id>"
TENANT_ID: "<your-tenant-id>"
ACR_NAME: "testing1231254984"
```

---

## 🐛 Troubleshooting

### Check Service Account

```bash
kubectl get serviceaccount azure-storage-sa -o yaml
```

### Check Pod Events

```bash
kubectl describe pod storage-upload-pod
```

### View Logs

```bash
kubectl logs -f pod/storage-upload-pod
```

### Verify Azure Permissions

```bash
az role assignment list --assignee <CLIENT_ID>
```

---

## 📚 Key Technologies

- **Java 11** - Application runtime
- **Azure Identity SDK** - DefaultAzureCredential
- **Azure Storage Blob SDK** - File upload
- **Kubernetes Workload Identity** - OIDC-based authentication
- **Docker** - Containerization
- **Maven** - Build tool

---

## 📖 For More Information

- Full README: [README.md](README.md)
- Azure Workload Identity Docs: https://learn.microsoft.com/en-us/azure/aks/workload-identity-overview
- Azure Identity SDK: https://github.com/Azure/azure-sdk-for-java
- Azure Storage Blob SDK: https://learn.microsoft.com/en-us/java/api/overview/azure/storage-blob-readme

---

## ✨ Next Steps

1. ✅ Review the code: `AzureStorageUploadService.java`
2. ✅ Build locally: `mvn clean package`
3. ✅ Set up Azure: `./azure-setup.sh`
4. ✅ Build Docker: `./build-and-push.sh`
5. ✅ Deploy to K8s: `kubectl apply -f k8s-manifest.yaml`
6. ✅ Monitor: `kubectl logs -f pod/storage-upload-pod`

---

## 🎯 File Upload Path

```
Pod creates file: yyyy-MM-dd_HH-mm-ss
    ↓
Uploads to Storage Account: testing1231254984
    ↓
Into Container: testing
    ↓
Final Path: https://testing1231254984.blob.core.windows.net/testing/2024-07-20_14-30-45
```

---

Made with ❤️ for secure, secret-free Azure authentication
