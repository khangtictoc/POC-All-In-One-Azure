# POC

## Target

Pod is authenticated with Azure Entra ID could upload a timestamped file into Blob Container `testing` in Azure Storage Account `testing1231254984`

**Prerequisite**: Main initial Azure/Microsoft account

## Implementation

### 1. Code base

**Go to main project**

cd `java-sa-wi-app`

Build & Push Docker Image

- Registry: docker.io (Docker Hub)
- Repo: darkhero101

`docker build --no-cache -t docker.io/darkhero101/java-sa-wi-service:4.0.0 .`

`docker push  docker.io/darkhero101/java-sa-wi-service:4.0.0`

### 2. Azure Portal

Create Storage Account

![alt text](https://cdn.jsdelivr.net/gh/khangtictoc/POC-All-In-One-Azure@main/azure-workload-identity/images/storage-account/image1.png)

Create Blob Container

- Inside Storage Account → Containers → Create Container
- Name: `testing`
- Public access level: Private (no anonymous access)

![alt text](https://cdn.jsdelivr.net/gh/khangtictoc/POC-All-In-One-Azure@main/azure-workload-identity/images/storage-account/image2.png)

Config JSON (Verified)

```json
{
  "apiVersion": "2026-04-01",
  "id": "/subscriptions/ac90b42a-8ba9-48f5-9479-94dfd054e40d/resourceGroups/dummy1/providers/Microsoft.Storage/storageAccounts/testing1231254984",
  "name": "testing1231254984",
  "type": "microsoft.storage/storageaccounts",
  "sku": {
    "name": "Standard_LRS",
    "tier": "Standard"
  },
  "kind": "StorageV2",
  "location": "eastus",
  "tags": {},
  "properties": {
    "allowCrossTenantDelegationSas": false,
    "dualStackEndpointPreference": {
      "defaultDualStackEndpoints": false,
      "publishIpv4Endpoint": false,
      "publishIpv6Endpoint": false
    },
    "dnsEndpointType": "Standard",
    "defaultToOAuthAuthentication": false,
    "publicNetworkAccess": "Enabled",
    "keyCreationTime": {
      "key1": "2026-07-19T20:35:00.3988147Z",
      "key2": "2026-07-19T20:35:00.3988147Z"
    },
    "allowCrossTenantReplication": false,
    "privateEndpointConnections": [],
    "minimumTlsVersion": "TLS1_2",
    "allowBlobPublicAccess": false,
    "allowSharedKeyAccess": true,
    "networkAcls": {
      "ipv6Rules": [],
      "bypass": "AzureServices",
      "virtualNetworkRules": [],
      "ipRules": [],
      "defaultAction": "Allow"
    },
    "supportsHttpsTrafficOnly": true,
    "encryption": {
      "requireInfrastructureEncryption": false,
      "services": {
        "file": {
          "keyType": "Account",
          "enabled": true,
          "lastEnabledTime": "2026-07-19T20:35:00.4024234Z"
        },
        "blob": {
          "keyType": "Account",
          "enabled": true,
          "lastEnabledTime": "2026-07-19T20:35:00.4024234Z"
        }
      },
      "keySource": "Microsoft.Storage"
    },
    "accessTier": "Hot",
    "provisioningState": "Succeeded",
    "creationTime": "2026-07-19T20:34:59.9557969Z",
    "primaryEndpoints": {
      "dfs": "https://testing1231254984.dfs.core.windows.net/",
      "web": "https://testing1231254984.z13.web.core.windows.net/",
      "blob": "https://testing1231254984.blob.core.windows.net/",
      "queue": "https://testing1231254984.queue.core.windows.net/",
      "table": "https://testing1231254984.table.core.windows.net/",
      "file": "https://testing1231254984.file.core.windows.net/"
    },
    "primaryLocation": "eastus",
    "statusOfPrimary": "available"
  }
}
```

### 3. Storage Account Setup

Create Managed Identity for Service (Pod) to consume

![alt text](https://cdn.jsdelivr.net/gh/khangtictoc/POC-All-In-One-Azure@main/azure-workload-identity/images/storage-account/image4.png)

**Grant RBAC Role to Managed Identity**

Azure Portal

![alt text](https://cdn.jsdelivr.net/gh/khangtictoc/POC-All-In-One-Azure@main/azure-workload-identity/images/storage-account/image3.png)

Or CLI

```bash
# Grant Storage Blob Data Contributor role to the Managed Identity (must-have)
# This allows the identity to read/write/delete blobs in the container
az role assignment create \
  --role "Owner" \ # FOR TESTING ONLY
  --assignee <identity-client-id> \
  --scope /subscriptions/ac90b42a-8ba9-48f5-9479-94dfd054e40d/resourceGroups/dummy1/providers/Microsoft.Storage/storageAccounts/testing1231254984

az role assignment create \
 --role "Storage Blob Data Contributor" \ # MUST-HAVE ROLE
 --assignee <identity-client-id> \
 --scope /subscriptions/ac90b42a-8ba9-48f5-9479-94dfd054e40d/resourceGroups/dummy1/providers/Microsoft.Storage/storageAccounts/testing1231254984
```

Verify role assignment:

```bash
az role assignment list \
  --assignee <identity-client-id> \
  --scope /subscriptions/ac90b42a-8ba9-48f5-9479-94dfd054e40d/resourceGroups/dummy1/providers/Microsoft.Storage/storageAccounts/testing1231254984
```

---

**Create Federated Credential for Service Account**

Azure Portal

![alt text](https://cdn.jsdelivr.net/gh/khangtictoc/POC-All-In-One-Azure@main/azure-workload-identity/images/storage-account/image5.png)

Or

```bash
az identity federated-credential create \
  --resource-group dummy1 \
  --identity-name <identity-name> \
  --name azure-storage-fed-cred \
  --issuer https://oidcToken.blob.core.windows.net/<SUBSCRIPTION_ID>/discovery/v1.0/keys \
  --subject system:serviceaccount:testing:testing \
  --audiences api://AzureADTokenExchange
```

### 4. Deploy

```bash
cd azure-workload-identity-sa-app
```

```bash
kubectl create namespace testing
kubectl apply -f . -n testing
```

Verify running service, upload a timestamped file then auto completed

![alt text](https://cdn.jsdelivr.net/gh/khangtictoc/POC-All-In-One-Azure@main/azure-workload-identity/images/storage-account/image7.png)

Verify logs

![alt text](https://cdn.jsdelivr.net/gh/khangtictoc/POC-All-In-One-Azure@main/azure-workload-identity/images/storage-account/image8.png)

Verify uploaded blob

```bash
az storage blob list \
  --account-name testing1231254984 \
  --container-name testing \
  --auth-mode login \
  --output table
```

![alt text](https://cdn.jsdelivr.net/gh/khangtictoc/POC-All-In-One-Azure@main/azure-workload-identity/images/storage-account/image6.png)

Official Docs: [Use Azure AD workload identity with Azure Kubernetes Service (AKS)](https://learn.microsoft.com/en-us/azure/aks/workload-identity-overview)
