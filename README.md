# AKS + Terraform + GitHub Actions Learning Repository


A comprehensive learning repository that demonstrates modern cloud-native development practices using **Terraform**, **GitHub Actions**, and **Azure** to deploy a containerized application to Kubernetes.


## What This Repository Does


This project automates the complete lifecycle of deploying a containerized application to Azure Kubernetes Service (AKS):


1. Creates an **Azure Container Registry (ACR)** for storing Docker images
2. Builds and pushes a containerized Java web application
3. Deploys an **Azure Kubernetes Service (AKS)** cluster
4. Runs the application with automatic image pulling from ACR
5. Exposes the application via a public LoadBalancer


**Result**: A running web service that responds with `Hello, World!`


➡️ Jump to: [How it works](#how-it-works) | [Quickstart](#quickstart)


## Features


- Infrastructure as Code (IaC) using Terraform
- Secure authentication via Azure OIDC (no stored credentials)
- Remote state management with Azure Storage
- Separate deployment workflows for ACR and AKS
- Automated Docker image building and pushing
- Kubernetes deployment with LoadBalancer service
- Manual approval gates via GitHub Environments
- Support for plan, apply, and destroy operations
- Git SHA-based image tagging for traceability


## Table of contents


- [Features](#features)
- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Technology Stack](#technology-stack)
- [Quickstart](#quickstart)
- [Architecture](#architecture)
- [Set up the GitHub repo](#set-up-the-github-repo)
- [Configuration](#configuration)
- [Deploy](#deploy)
- [Verify](#verify)
- [Useful Commands](#useful-commands)
- [Troubleshooting](#troubleshooting)
- [Destroy](#destroy)
- [How it works](#how-it-works)
- [Local workflow (optional)](#local-workflow-optional)
- [Cost Considerations](#cost-considerations)
- [Security Best Practices](#security-best-practices)
- [Notes and constraints](#notes-and-constraints)
- [Additional Resources](#additional-resources)


## Overview


This repository is organized into two Terraform deployments (ACR first, then AKS), plus a minimal Java web app and Dockerfile.


### Repository Structure


```
aks-hello-world-poc/
│
├── .github/
│   └── workflows/                      # GitHub Actions CI/CD pipelines
│       ├── deploy_acr.yaml            # Workflow to deploy ACR
│       │                              #  • Terraform plan/apply/destroy
│       │                              #  • Uses Azure OIDC auth
│       │                              #  • Manual approval via Environment
│       │
│       └── deploy_aks.yaml            # Workflow to deploy AKS + app
│                                      #  • Builds & pushes Docker image
│                                      #  • Terraform plan/apply/destroy
│                                      #  • Deploys Kubernetes resources
│
├── terraform/
│   │
│   ├── acr/                           # ACR Infrastructure Module
│   │   ├── main.tf                    #  • Creates Azure Container Registry
│   │   │                              #  • Configures Basic/Standard tier
│   │   │                              #  • Tags resources
│   │   ├── providers.tf               #  • AzureRM provider config
│   │   │                              #  • Terraform version constraints
│   │   ├── variables.tf               #  • Input variable definitions
│   │   │                              #  • Validation rules
│   │   └── vars/
│   │       └── poc.tfvars             #  • Environment-specific values
│   │                                  #  • Resource group, location, tags
│   │
│   └── aks/                           # AKS Infrastructure Module
│       ├── main.tf                    #  • Creates AKS cluster
│       │                              #  • Creates Kubernetes Deployment
│       │                              #  • Creates Kubernetes Service
│       │                              #  • Assigns AcrPull role to AKS
│       ├── providers.tf               #  • AzureRM + Kubernetes providers
│       │                              #  • Dynamic kubeconfig
│       ├── variables.tf               #  • Input variable definitions
│       │                              #  • Node size, count, image details
│       └── vars/
│           └── poc.tfvars             #  • Environment-specific values
│                                      #  • AKS configuration
│
├── Dockerfile                         # Multi-stage container build
│                                      #  • Stage 1: Compile Java app
│                                      #  • Stage 2: Runtime image (OpenJDK)
│                                      #  • Exposes port 8080
│
├── HelloWorld.java                    # Simple HTTP server application
│                                      #  • Listens on port 8080
│                                      #  • Responds "Hello, World!" on GET /
│
└── README.md                          # This file
```


### What Gets Created in Azure


When fully deployed, this creates:


**Resource Group** (must exist beforehand)
- ├─ **Azure Container Registry**
- │  └─ Docker image repository (helloworld-java:SHA)
- │
- ├─ **Azure Kubernetes Service**
- │  ├─ System node pool (1 node, Standard_D2s_v3)
- │  ├─ Kubernetes Deployment (hello-world app)
- │  ├─ Kubernetes Service (LoadBalancer type)
- │  └─ Role assignment (AcrPull permission)
- │
- └─ **Azure Load Balancer** (auto-created)
-    └─ Public IP address


**Separate Resource (outside RG):**
- **Azure Storage Account** (for Terraform state backend)
## Prerequisites


You’ll need:


- **An Azure subscription**
- **An existing Azure Resource Group**
  - Terraform looks it up; it does not create it.
- **An Azure Storage account + blob container** for Terraform remote state
- **Azure AD app registration / federated credentials for GitHub OIDC**
  - Represented in GitHub as `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, and `AZURE_SUBSCRIPTION_ID`
- **A public (or Enterprise) GitHub repository** with Actions enabled


> This repo is a learning exercise and keeps infrastructure minimal and explicit.


## Technology Stack


### Infrastructure & Cloud
- **Azure**: Cloud platform hosting all resources
- **Terraform**: Infrastructure as Code (IaC) tool for provisioning Azure resources
- **Azure Container Registry (ACR)**: Private Docker registry
- **Azure Kubernetes Service (AKS)**: Managed Kubernetes cluster
- **Azure Storage**: Backend for Terraform state files


### Container & Orchestration
- **Docker**: Container runtime and image builder
- **Kubernetes**: Container orchestration platform
- **kubectl**: Kubernetes command-line tool (for manual operations)


### CI/CD & Automation
- **GitHub Actions**: CI/CD platform for automated workflows
- **Azure OIDC**: Secure workload identity federation (no static credentials)


### Application
- **Java**: Simple HTTP server application
- **OpenJDK**: Java runtime in the container


### Tool Versions
This repository has been tested with:
- Terraform >= 1.5.0
- Azure CLI >= 2.50.0
- kubectl >= 1.27.0
- Docker >= 24.0.0


## Quickstart


1) Create a new fork of this repository as your starting point.
2) Set up GitHub repo **variables** and **secrets**: see [Configuration](#configuration).  
3) Deploy ACR: see [Deploy](#deploy).  
4) Deploy AKS + the app: see [Deploy](#deploy).  
5) Confirm the response: see [Verify](#verify).


## Architecture


### High-Level Architecture


```
┌─────────────────────────────────────────────────────────────────┐
│                         GitHub Actions                          │
│  ┌──────────────────────┐      ┌──────────────────────┐        │
│  │  Deploy ACR Workflow │      │  Deploy AKS Workflow │        │
│  │  • Build Docker img  │      │  • Build & Push img  │        │
│  │  • Terraform Plan    │      │  • Terraform Plan    │        │
│  │  • Terraform Apply   │      │  • Terraform Apply   │        │
│  └──────────┬───────────┘      └──────────┬───────────┘        │
│             │                              │                     │
│             └──────────┐      ┌────────────┘                    │
│                        │      │                                 │
│                    Azure OIDC Authentication                    │
└────────────────────────┼──────┼──────────────────────────────────┘
                         │      │
                         ▼      ▼
        ┌────────────────────────────────────────────┐
        │           Azure Subscription               │
        │                                            │
        │  ┌──────────────────────────────────────┐ │
        │  │     Azure Resource Group             │ │
        │  │                                      │ │
        │  │  ┌────────────────────────────────┐ │ │
        │  │  │  Azure Container Registry      │ │ │
        │  │  │  • Stores Docker images        │ │ │
        │  │  │  • Tagged by Git SHA           │ │ │
        │  │  └────────────┬───────────────────┘ │ │
        │  │               │ AcrPull             │ │
        │  │               │ Role                │ │
        │  │               │                     │ │
        │  │  ┌────────────▼───────────────────┐ │ │
        │  │  │  Azure Kubernetes Service      │ │ │
        │  │  │  ┌──────────────────────────┐  │ │ │
        │  │  │  │  Kubernetes Deployment   │  │ │ │
        │  │  │  │  • Runs hello-world app  │  │ │ │
        │  │  │  │  • Pulls from ACR        │  │ │ │
        │  │  │  └──────────┬───────────────┘  │ │ │
        │  │  │             │                   │ │ │
        │  │  │  ┌──────────▼───────────────┐  │ │ │
        │  │  │  │  Kubernetes Service      │  │ │ │
        │  │  │  │  • Type: LoadBalancer    │  │ │ │
        │  │  │  │  • Port: 80 → 8080       │  │ │ │
        │  │  │  └──────────┬───────────────┘  │ │ │
        │  │  └─────────────┼──────────────────┘ │ │
        │  └────────────────┼────────────────────┘ │
        │                   │                       │
        │  ┌────────────────▼────────────────────┐ │
        │  │  Azure Load Balancer                │ │
        │  │  • Public IP assigned               │ │
        │  │  • Routes traffic to AKS pods       │ │
        │  └────────────┬────────────────────────┘ │
        └───────────────┼──────────────────────────┘
                        │
                        ▼
                 ┌──────────────┐
                 │   Internet   │
                 │    Users     │
                 └──────────────┘
```


### Deployment Flow


1. **ACR Deployment** (First)
   - GitHub Actions authenticates via OIDC
   - Terraform provisions Azure Container Registry
   - State stored in Azure Storage backend


2. **AKS Deployment** (Second)
   - Application code is built into Docker image
   - Image pushed to ACR with Git SHA tag
   - Terraform provisions AKS cluster
   - Terraform applies Kubernetes manifests (Deployment + Service)
   - AcrPull role assigned to AKS kubelet identity
   - LoadBalancer provisions public IP


3. **Runtime**
   - AKS pulls image from ACR
   - Pods run the containerized application
   - LoadBalancer routes external traffic to pods
   - Application responds on port 8080 internally (exposed as port 80)


### Component Relationships


There are two Terraform deployments:


- `terraform/acr` creates the Azure Container Registry
- `terraform/aks` creates AKS and Kubernetes resources, and references ACR by name


**Key Relationships:**
- AKS **depends on** ACR (must deploy ACR first)
- AKS kubelet identity has **AcrPull** role on ACR
- Kubernetes Deployment references ACR image path
- Kubernetes Service exposes Deployment via LoadBalancer


Module docs:
- [terraform/acr](terraform/acr/README.md)
- [terraform/aks](terraform/aks/README.md)


## Set up the GitHub repo


### Step-by-step Setup


#### 1. Create GitHub Repository
```bash
# Option A: Fork this repository (recommended for learning)
# Click "Fork" button in GitHub UI


# Option B: Create new repository and push code
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/<YOUR_USERNAME>/<REPO_NAME>.git
git push -u origin main
```


#### 2. Set Up Azure OIDC Authentication


Follow these steps to configure GitHub to authenticate with Azure using OIDC:


**2.1. Create Azure AD Application**
```bash
# Login to Azure
az login


# Create AD app
az ad app create --display-name "GitHub-Actions-AKS-POC"


# Note the Application (client) ID
```


**2.2. Create Service Principal**
```bash
# Create service principal
az ad sp create --id <APPLICATION_ID>


# Assign Contributor role at subscription level (or resource group level for least privilege)
az role assignment create \
  --assignee <APPLICATION_ID> \
  --role Contributor \
  --scope /subscriptions/<SUBSCRIPTION_ID>


# If using resource group scope:
az role assignment create \
  --assignee <APPLICATION_ID> \
  --role Contributor \
  --scope /subscriptions/<SUBSCRIPTION_ID>/resourceGroups/<RESOURCE_GROUP_NAME>


# Grant Storage Blob Data Contributor for Terraform state
az role assignment create \
  --assignee <APPLICATION_ID> \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<SUBSCRIPTION_ID>/resourceGroups/<STORAGE_RG>/providers/Microsoft.Storage/storageAccounts/<STORAGE_ACCOUNT>
```


**2.3. Configure Federated Credentials**
```bash
# Create federated credential for main branch
az ad app federated-credential create \
  --id <APPLICATION_ID> \
  --parameters '{
    "name": "github-actions-main",
    "issuer": "https://token.actions.githubusercontent.com",
    "subject": "repo:<ORG>/<REPO>:ref:refs/heads/main",
    "audiences": ["api://AzureADTokenExchange"]
  }'


# Optional: Create for pull requests
az ad app federated-credential create \
  --id <APPLICATION_ID> \
  --parameters '{
    "name": "github-actions-pr",
    "issuer": "https://token.actions.githubusercontent.com",
    "subject": "repo:<ORG>/<REPO>:pull_request",
    "audiences": ["api://AzureADTokenExchange"]
  }'
```


#### 3. Create GitHub Environment (Optional but Recommended)


1. Go to repository **Settings** → **Environments**
2. Click **New environment**
3. Name it `poc`
4. Configure environment protection rules:
   - ✅ **Required reviewers**: Add yourself or team members
   - ✅ **Wait timer**: Optional delay before deployment
   - ⚙️ **Deployment branches**: Limit to main branch only


This adds a manual approval gate before `terraform apply` runs.


#### 4. Configure Repository Secrets and Variables


See [Configuration](#configuration) section below for detailed list.


#### 5. Verify Setup


Run this checklist before deploying:
- [ ] GitHub repository created and code pushed
- [ ] Azure AD app and service principal created
- [ ] Federated credentials configured
- [ ] Service principal has Contributor role
- [ ] Service principal has Storage Blob Data Contributor role
- [ ] GitHub Environment `poc` created (optional)
- [ ] All repository secrets configured
- [ ] All repository variables configured
- [ ] Storage account and container exist for Terraform backend
- [ ] Resource group exists in Azure


### Optional: Require manual approval before apply
Both workflows use `environment: name: poc` on the apply job. If you create a GitHub Environment named `poc`, you can configure:
- **Required reviewers** (manual approval gates)
- **Environment secrets/vars** (not used here, but supported)
- **Deployment branches** (restrict to main/production branches)


**Benefits of using Environments:**
- Prevents accidental deployments
- Provides audit trail of approvals
- Allows environment-specific configuration
- Supports deployment protection rules


## Configuration


### Prerequisites: Azure Resources


This repo assumes the following already exist in your Azure subscription:


| Resource | Purpose | How to Create |
|----------|---------|---------------|
| **Resource Group** | Container for ACR and AKS | `az group create --name <RG_NAME> --location <LOCATION>` |
| **Storage Account** | Terraform state backend | `az storage account create --name <STORAGE> --resource-group <RG> --location <LOCATION> --sku Standard_LRS` |
| **Storage Container** | Holds `.tfstate` files | `az storage container create --name tfstate --account-name <STORAGE>` |
| **Azure AD App** | OIDC authentication | See [Set up the GitHub repo](#set-up-the-github-repo) |
| **Service Principal** | Azure authentication | Created from Azure AD app |
| **Federated Credentials** | GitHub OIDC trust | Configured on Azure AD app |


### GitHub Repository Variables


Navigate to **Settings** → **Secrets and variables** → **Actions** → **Variables** tab


Create these **repository-level variables**:


| Variable | Description | Example Value | Required |
|----------|-------------|---------------|----------|
| `ACR_NAME` | Azure Container Registry name (globally unique) | `myacrpoc2024` | ✅ Yes |
| `IMAGE_NAME` | Container image repository name | `helloworld-java` | ✅ Yes |


**Important Notes:**
- ACR names must be globally unique across all of Azure
- ACR names can only contain alphanumeric characters (no hyphens or underscores)
- These are exported as `TF_VAR_acr_name` and `TF_VAR_image_name` to Terraform


### GitHub Repository Secrets


Navigate to **Settings** → **Secrets and variables** → **Actions** → **Secrets** tab


Create these **repository-level secrets**:


#### Azure OIDC Authentication


| Secret | Description | How to Get |
|--------|-------------|------------|
| `AZURE_CLIENT_ID` | Application (client) ID of Azure AD app | `az ad app list --display-name "GitHub-Actions-AKS-POC" --query "[0].appId" -o tsv` |
| `AZURE_TENANT_ID` | Azure AD tenant ID | `az account show --query tenantId -o tsv` |
| `AZURE_SUBSCRIPTION_ID` | Azure subscription ID | `az account show --query id -o tsv` |


#### Terraform Backend (Azure Storage)


| Secret | Description | How to Get |
|--------|-------------|------------|
| `BACKEND_AZURE_RESOURCE_GROUP_NAME` | Resource group containing storage account | Your chosen RG name |
| `BACKEND_AZURE_STORAGE_ACCOUNT_NAME` | Storage account name for Terraform state | Your storage account name |
| `BACKEND_AZURE_STORAGE_ACCOUNT_CONTAINER_NAME` | Container name within storage account | Usually `tfstate` |


### Configuration Checklist


Before running workflows, verify:


**Azure Resources**
- [ ] Resource group exists: `az group show --name <RG_NAME>`
- [ ] Storage account exists: `az storage account show --name <STORAGE_NAME>`
- [ ] Storage container exists: `az storage container show --name tfstate --account-name <STORAGE_NAME>`
- [ ] Service principal has correct permissions


**GitHub Configuration**
- [ ] Repository variables set: `ACR_NAME`, `IMAGE_NAME`
- [ ] Repository secrets set: All 6 secrets configured
- [ ] Environment `poc` created (optional but recommended)
- [ ] Required reviewers added to environment (optional)


**Verify Access**
```bash
# Test Azure login with service principal
az login --service-principal \
  --username <AZURE_CLIENT_ID> \
  --password <AZURE_CLIENT_SECRET> \
  --tenant <AZURE_TENANT_ID>


# For OIDC (no password), verify federated credentials exist
az ad app federated-credential list --id <AZURE_CLIENT_ID>
```


## Deploy


> ⚠️ **Order matters**: deploy **ACR first**, then **AKS**.


### Deployment Process Overview


```
1. Deploy ACR         2. Deploy AKS + App        3. Verify
   ↓                     ↓                          ↓
[ACR created]  →  [Docker build & push]  →  [Test endpoint]
                  [AKS cluster created]
                  [K8s resources created]
                  [App running]
```


### Step 1: Deploy ACR


**1.1. Navigate to Actions**
- Go to your repository → **Actions** tab
- Select workflow: **Deploy ACR Terraform**


**1.2. Run Workflow**
- Click **Run workflow** dropdown
- Choose branch: `main`
- Select action: `plan -> apply`
- Select environment: `poc`
- Click **Run workflow** button


**1.3. Review Plan**
- Wait for `plan` job to complete
- Review Terraform plan output in logs
- Check what resources will be created


**1.4. Approve Apply** (if using Environment protection)
- Navigate to the workflow run
- Click **Review deployments**
- Select `poc` environment
- Click **Approve and deploy**


**1.5. Wait for Completion**
- The `apply` job will run
- ACR will be created in Azure
- State file saved to Azure Storage


**Expected Result:**
```
✅ Azure Container Registry created
✅ Resource tagged with environment and project info
✅ Registry ready to receive Docker images
```


### Step 2: Deploy AKS + Application


**2.1. Navigate to Actions**
- Go to your repository → **Actions** tab
- Select workflow: **Deploy AKS Terraform**


**2.2. Run Workflow**
- Click **Run workflow** dropdown
- Choose branch: `main`
- Select action: `plan -> apply`
- Select environment: `poc`
- Click **Run workflow** button


**2.3. What Happens Automatically**


The workflow performs these steps:


```
Phase 1: Build & Push
├─ Checkout code
├─ Authenticate to Azure (OIDC)
├─ Build Docker image from Dockerfile
├─ Tag image with Git SHA (e.g., abc123f)
└─ Push image to ACR


Phase 2: Plan
├─ Initialize Terraform
├─ Download AzureRM + Kubernetes providers
├─ Configure remote state backend
├─ Run terraform plan
└─ Display planned changes


Phase 3: Apply (requires approval)
├─ Wait for manual approval (if Environment configured)
├─ Run terraform apply
├─ Create AKS cluster
├─ Assign AcrPull role to AKS
├─ Create Kubernetes Deployment
├─ Create Kubernetes Service (LoadBalancer)
└─ Provision public IP
```


**2.4. Review Plan**
- Examine the Terraform plan output
- Verify ACR name, image tag, and AKS configuration
- Check node count and VM size


**2.5. Approve Apply**
- Review and approve deployment (if using Environment)


**2.6. Wait for Completion**
- AKS cluster provisioning takes ~5-10 minutes
- Kubernetes resources deploy in ~1-2 minutes
- LoadBalancer IP assignment takes ~2-5 minutes


**Expected Result:**
```
✅ Docker image built and pushed to ACR
✅ AKS cluster running with 1 node
✅ Kubernetes Deployment created (1 replica)
✅ Kubernetes Service created with public IP
✅ Application responding to HTTP requests
```


### Deployment Timeline


| Step | Duration | Notes |
|------|----------|-------|
| ACR creation | 1-2 min | Usually fast |
| Docker build & push | 2-3 min | Depends on image size |
| AKS cluster creation | 5-10 min | Azure provisions VMs and control plane |
| Kubernetes deployment | 1-2 min | Pod startup and image pull |
| LoadBalancer IP | 2-5 min | Azure provisions public IP |
| **Total** | **10-20 min** | First-time deployment |


### Monitoring Deployment Progress


**From GitHub Actions:**
```
• View live logs for each job
• See Terraform output in real-time
• Check for errors or warnings
```


**From Azure Portal:**
```
• Navigate to Resource Group
• View ACR → Repositories → Check for image
• View AKS → Overview → Check provisioning state
• View AKS → Workloads → Check pod status
```


**From Azure CLI:**
```bash
# Check ACR
az acr show --name <ACR_NAME> --query provisioningState


# Check AKS
az aks show --resource-group <RG> --name <AKS_NAME> --query provisioningState


# Get AKS credentials
az aks get-credentials --resource-group <RG> --name <AKS_NAME>


# Check Kubernetes resources
kubectl get all
```


## Verify


### Method 1: Using kubectl (Recommended)


**1. Get AKS credentials**
```bash
az aks get-credentials \
  --resource-group <RESOURCE_GROUP> \
  --name <AKS_CLUSTER_NAME> \
  --overwrite-existing
```


**2. Check service status**
```bash
kubectl get services


# Example output:
# NAME                 TYPE           CLUSTER-IP     EXTERNAL-IP      PORT(S)        AGE
# helloworld-service   LoadBalancer   10.0.123.45    20.123.45.67     80:30123/TCP   5m
# kubernetes           ClusterIP      10.0.0.1       <none>           443/TCP        15m
```


**3. Wait for EXTERNAL-IP**
```bash
# If EXTERNAL-IP shows <pending>, wait a few minutes
kubectl get service helloworld-service --watch


# Or check continuously
while true; do kubectl get service helloworld-service | grep -v EXTERNAL-IP | awk '{print $4}'; sleep 5; done
```


**4. Test the endpoint**
```bash
# Get the external IP
EXTERNAL_IP=$(kubectl get service helloworld-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')


# Test with curl
curl http://$EXTERNAL_IP/


# Expected output:
# Hello, World!
```


### Method 2: Using Azure Portal


**1. Navigate to AKS cluster**
- Open [Azure Portal](https://portal.azure.com)
- Go to **Resource Groups** → Your resource group
- Click on your AKS cluster


**2. View Kubernetes resources**
- In left menu, click **Services and ingresses**
- Find service named `helloworld-service`
- Copy the **External IP** address


**3. Test in browser or curl**
```bash
curl http://<EXTERNAL_IP>/
```


### Method 3: Using Azure CLI


```bash
# Get AKS credentials (if not done already)
az aks get-credentials --resource-group <RG> --name <AKS_NAME>


# Get external IP
az aks show \
  --resource-group <RG> \
  --name <AKS_NAME> \
  --query "agentPoolProfiles[0].count" -o tsv


# Or use kubectl through az command
az aks command invoke \
  --resource-group <RG> \
  --name <AKS_NAME> \
  --command "kubectl get service helloworld-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}'"
```


### Expected Response


**Success Response:**
```
Hello, World!
```


**Characteristics:**
- HTTP Status Code: `200 OK`
- Content-Type: `text/plain`
- Response time: < 100ms (typically)
- No HTML formatting (plain text response)


### Testing Tips


**Test with various methods:**
```bash
# Basic curl
curl http://<EXTERNAL_IP>/


# Include headers
curl -v http://<EXTERNAL_IP>/


# Test response time
curl -w "\nTime: %{time_total}s\n" http://<EXTERNAL_IP>/


# Test from browser (won't render as HTML but will show plain text)
http://<EXTERNAL_IP>/


# Load testing (optional - use carefully)
ab -n 100 -c 10 http://<EXTERNAL_IP>/
```


**Test different endpoints:**
```bash
# Root endpoint (should work)
curl http://<EXTERNAL_IP>/


# Other paths (should also return "Hello, World!" - app only has one route)
curl http://<EXTERNAL_IP>/test
curl http://<EXTERNAL_IP>/api
```


### Verification Checklist


After deployment, verify all components:


**Infrastructure Layer:**
- [ ] ACR exists and contains image
- [ ] AKS cluster is running
- [ ] AKS has 1 or more nodes in "Ready" state
- [ ] AcrPull role assigned to AKS


**Kubernetes Layer:**
- [ ] Deployment exists with desired replicas
- [ ] Pods are in "Running" state
- [ ] Service exists with type LoadBalancer
- [ ] External IP is assigned (not `<pending>`)


**Application Layer:**
- [ ] Pods pull image successfully from ACR
- [ ] Application starts without errors
- [ ] Application responds to HTTP requests
- [ ] Response is "Hello, World!"


**Network Layer:**
- [ ] LoadBalancer has public IP
- [ ] Port 80 is accessible from internet
- [ ] No firewall blocking traffic
- [ ] DNS resolution works (if using domain)


### Detailed Verification Commands


```bash
# 1. Verify ACR image
az acr repository show-tags \
  --name <ACR_NAME> \
  --repository <IMAGE_NAME> \
  --orderby time_desc \
  --output table


# 2. Verify AKS nodes
kubectl get nodes -o wide


# 3. Verify deployment
kubectl describe deployment helloworld-deployment


# 4. Verify pods
kubectl get pods -o wide
kubectl logs -l app=helloworld --tail=50


# 5. Verify service
kubectl describe service helloworld-service


# 6. Verify role assignment
az role assignment list \
  --scope $(az acr show --name <ACR_NAME> --query id -o tsv) \
  --output table


# 7. Test internal connectivity (from within cluster)
kubectl run test-pod --image=busybox -it --rm --restart=Never -- \
  wget -O- http://helloworld-service


# 8. Check resource usage
kubectl top nodes
kubectl top pods
```


## Useful Commands


### Get AKS Credentials
```bash
az aks get-credentials --resource-group <RESOURCE_GROUP> --name <AKS_CLUSTER_NAME>
```


### Check Kubernetes Resources
```bash
# View all pods
kubectl get pods


# View deployments
kubectl get deployments


# View services and external IP
kubectl get services


# View detailed service information
kubectl describe service helloworld-service


# View pod logs
kubectl logs -l app=helloworld


# Check pod details and events
kubectl describe pod -l app=helloworld
```


### ACR Operations
```bash
# Login to ACR
az acr login --name <ACR_NAME>


# List images in ACR
az acr repository list --name <ACR_NAME>


# List tags for an image
az acr repository show-tags --name <ACR_NAME> --repository <IMAGE_NAME>


# Manually push an image (if needed)
docker tag <IMAGE_NAME>:latest <ACR_NAME>.azurecr.io/<IMAGE_NAME>:latest
docker push <ACR_NAME>.azurecr.io/<IMAGE_NAME>:latest
```


### Kubernetes Troubleshooting
```bash
# Check cluster nodes
kubectl get nodes


# Check all resources in default namespace
kubectl get all


# View recent events
kubectl get events --sort-by='.lastTimestamp'


# Execute commands inside a pod
kubectl exec -it <POD_NAME> -- /bin/sh


# Port-forward for local testing (alternative to LoadBalancer)
kubectl port-forward service/helloworld-service 8080:80
```


### Terraform State Operations
```bash
# List resources in state
terraform state list


# Show specific resource
terraform state show <RESOURCE_ADDRESS>


# Refresh state without changes
terraform refresh -var-file="vars/poc.tfvars"
```


## Troubleshooting


### Common Issues


#### 1. LoadBalancer IP is stuck in "Pending"
**Symptom**: `kubectl get services` shows `<pending>` for EXTERNAL-IP


**Possible causes**:
- Cloud provider is still provisioning the load balancer (wait 2-5 minutes)
- AKS cluster doesn't have proper permissions
- Network security group blocking traffic
- Azure subscription quota exceeded


**Solution**:
```bash
# Check service events
kubectl describe service helloworld-service


# Check AKS cluster status
az aks show --resource-group <RG> --name <AKS_NAME> --query provisioningState
```


#### 2. Image Pull Errors
**Symptom**: Pods stuck in `ImagePullBackOff` or `ErrImagePull`


**Possible causes**:
- ACR not accessible from AKS
- AcrPull role assignment missing or incorrect
- Image name or tag incorrect
- ACR firewall blocking AKS


**Solution**:
```bash
# Check pod events for detailed error
kubectl describe pod <POD_NAME>


# Verify AcrPull role assignment exists
az role assignment list --assignee <AKS_KUBELET_IDENTITY> --scope <ACR_RESOURCE_ID>


# Test ACR connectivity from local machine
az acr login --name <ACR_NAME>
```


#### 3. Terraform Backend Access Denied
**Symptom**: Terraform init fails with storage account access error


**Solution**:
- Verify Azure credentials are correct
- Check that storage account and container exist
- Ensure the identity has "Storage Blob Data Contributor" role on the storage account


#### 4. Application Not Responding
**Symptom**: External IP accessible but returns connection refused or timeout


**Solution**:
```bash
# Check if pods are running
kubectl get pods


# Check pod logs
kubectl logs -l app=helloworld


# Test service internally first
kubectl run test-pod --image=busybox -it --rm -- wget -O- http://helloworld-service
```


#### 5. Workflow Fails on Docker Build
**Symptom**: GitHub Actions fails during Docker build step


**Solution**:
- Check Dockerfile syntax
- Verify HelloWorld.java compiles correctly
- Review workflow logs for specific error messages
- Ensure ACR name is correctly set in repository variables


### Getting Help


If you encounter issues:
1. Check GitHub Actions workflow logs for detailed error messages
2. Review Terraform plan output before applying
3. Use `kubectl describe` and `kubectl logs` for Kubernetes issues
4. Check Azure Portal for resource status and activity logs
5. Review this repository's Issues tab for similar problems


## Destroy


Both workflows also support `destroy`.


* The **ACR destroy** runs a normal `terraform destroy` for the ACR deployment.
* The **AKS destroy** is **targeted** to the AKS cluster, role assignment, and the Kubernetes resources created by this module. This helps avoid accidentally destroying unrelated resources that may exist in the same Resource Group.


> For a learning repo, it’s normal to destroy and recreate often. Just be aware that destroying cloud resources is permanent.


## How it works


### 1) The app is a tiny web server
- `HelloWorld.java` starts a simple HTTP server on port `8080`.
- When you visit `/`, it returns `Hello, World!`. It's important to note that this won't be visible in a browser because it isn't valid HTML.


### 2) Docker turns the app into an image
- `Dockerfile` compiles the Java file and packages it into a container image.
- An image is like a “frozen bundle” of your app + everything it needs to run.


### 3) ACR is where the image is stored
- **Azure Container Registry (ACR)** is a private place to store container images.
- The AKS workflow builds the image and pushes it to ACR.


### 4) AKS runs the image
- **Azure Kubernetes Service (AKS)** is a managed Kubernetes cluster.
- Terraform creates:
  - the AKS cluster
  - a Kubernetes **Deployment** (runs your container)
  - a Kubernetes **Service** of type **LoadBalancer** (gives you a public IP)


### 5) AKS is allowed to pull from ACR
- By default, AKS can’t pull private images from ACR.
- Terraform adds an Azure **role assignment** (`AcrPull`) so the cluster’s kubelet identity can pull images from your registry.


### 6) GitHub Actions runs the whole process
You start both workflows manually from the GitHub UI. Each workflow runs Terraform using:
- **OIDC authentication** to Azure (no long-lived Azure password in the repo)
- an **Azure Storage remote backend** for Terraform state
- a **plan → apply** flow (with optional manual approval via GitHub Environments)


### 7) Why ACR and AKS are separate workflows


ACR and AKS are deployed in **two separate Terraform configurations** and **two separate GitHub Actions workflows** on purpose:


- **They are independent layers.**  
  ACR is foundational infrastructure (a place to store images). AKS is the runtime environment (a cluster that pulls and runs images). Keeping them separate makes the dependency clear: **AKS needs ACR to exist first**, but ACR does not depend on AKS.


- **Different change frequency.**  
  You typically create ACR once and rarely change it. You may update the AKS workload often (new image tags, new app settings, Kubernetes changes). Separating workflows lets you redeploy AKS without touching ACR.


- **Cleaner, smaller Terraform state.**  
  Each Terraform deployment has its own remote state file:
  - ACR uses `acr_<environment>.tfstate`
  - AKS uses `aks_<environment>.tfstate`  
  Smaller, focused state files are easier to understand and reduce the “blast radius” of changes.


- **Safer applies and destroys.**  
  Having separate workflows reduces the chance that an AKS change accidentally modifies or destroys ACR (or vice versa). It also allows different destroy behavior (for example, the AKS workflow uses a targeted destroy).


- **Build/push fits naturally with the AKS deployment.**  
  The AKS workflow builds and pushes the Docker image to ACR and then deploys Kubernetes resources that reference that image tag. Keeping build/push next to the AKS apply makes the delivery flow easy to follow.


In short: **ACR is the image store; AKS is the image runner**—separating workflows keeps the learning repo easier to reason about and safer to operate.


➡️ Jump to: [Quickstart](#quickstart)


## Local workflow (optional)


If you want to run Terraform locally (instead of GitHub Actions), the key is to mirror what the workflows do:


* Use Azure OIDC or another supported `azurerm` auth method
* Configure the AzureRM backend at `terraform init` time
* Provide required variables (tfvars + `TF_VAR_*`)


> The GitHub Actions workflows are the “source of truth” for how this repo is intended to run. Local steps below are provided for convenience.


### Local: ACR


From repo root:


```bash
cd terraform/acr


# Backend config (replace placeholders)
terraform init -upgrade \
  -backend-config="resource_group_name=<BACKEND_RG>" \
  -backend-config="storage_account_name=<BACKEND_STORAGE_ACCOUNT>" \
  -backend-config="container_name=<BACKEND_CONTAINER>" \
  -backend-config="key=acr_poc.tfstate"


# Supply ACR name the same way the workflow does
export TF_VAR_acr_name="<ACR_NAME>"


terraform plan -var-file="vars/poc.tfvars"
terraform apply -var-file="vars/poc.tfvars"
```


### Local: AKS


From repo root:


```bash
cd terraform/aks


terraform init -upgrade \
  -backend-config="resource_group_name=<BACKEND_RG>" \
  -backend-config="storage_account_name=<BACKEND_STORAGE_ACCOUNT>" \
  -backend-config="container_name=<BACKEND_CONTAINER>" \
  -backend-config="key=aks_poc.tfstate"


# Supply shared values the same way the workflow does
export TF_VAR_acr_name="<ACR_NAME>"
export TF_VAR_image_name="<IMAGE_NAME>"


# Use any tag you pushed (the workflow uses the Git SHA)
terraform plan -var-file="vars/poc.tfvars" -var="image_tag=<TAG>"
terraform apply -var-file="vars/poc.tfvars" -var="image_tag=<TAG>"
```


> Local Docker build/push is intentionally not documented here; the GitHub Actions AKS workflow performs build/push automatically during apply.


## Cost Considerations


This infrastructure incurs costs while running. Here are the main cost components:


### Azure Resources (Estimated Monthly Costs)


**Azure Kubernetes Service (AKS)**
- **Control Plane**: Free (managed by Azure)
- **Worker Nodes**: Depends on VM size
  - Example: 1x Standard_D2s_v3 (2 vCPU, 8 GB RAM) ≈ $70-100/month
  - Scales with number and size of nodes


**Azure Container Registry (ACR)**
- **Basic tier**: ~$5/month (up to 10 GB storage)
- **Standard tier**: ~$20/month (up to 100 GB storage)
- Data transfer charges may apply


**Azure Load Balancer**
- **Basic SKU**: ~$0.005/hour + data processing charges ≈ $5-10/month
- **Standard SKU**: ~$0.025/hour + additional features ≈ $20-30/month


**Azure Storage (Terraform State)**
- Negligible cost (< $1/month for small state files)


**Total Estimated Cost**: $80-140/month for minimal setup


### Cost Optimization Tips


1. **Delete resources when not in use**
   - Run destroy workflows after testing
   - This is a learning environment—tear down between learning sessions


2. **Use smaller VM sizes for learning**
   - Standard_B2s is cheaper for non-production workloads
   - Adjust `node_size` in `terraform/aks/vars/poc.tfvars`


3. **Reduce node count**
   - Single node is sufficient for learning
   - Default is 1 node (check `node_count` in tfvars)


4. **Use Azure Free Credits**
   - New Azure accounts get $200 free credit for 30 days
   - Students can get $100/year with Azure for Students


5. **Monitor costs**
   - Use Azure Cost Management + Billing in Azure Portal
   - Set up budget alerts to avoid surprises


## Security Best Practices


### Authentication & Authorization


**✅ Current Implementation (Good)**
- Uses Azure OIDC/Workload Identity Federation (no long-lived credentials stored in GitHub)
- Terraform state stored securely in Azure Storage
- AKS uses managed identities for ACR access (AcrPull role)


**🔒 Additional Recommendations**


1. **Limit GitHub OIDC permissions**
   - Grant Azure service principal only necessary permissions
   - Use separate service principals for different environments


2. **Enable Azure RBAC on AKS**
   - Use Azure AD integration for kubectl access
   - Implement least-privilege access control


3. **Secure ACR access**
   - Consider enabling ACR firewall rules to allow only AKS subnet
   - Enable Azure Defender for container registries (production)


### Network Security


**⚠️ Current Setup (Learning Environment)**
- Application exposed via public LoadBalancer
- No network policies configured
- No Azure Firewall or Application Gateway


**🔒 Production Recommendations**


1. **Use Azure Application Gateway or NGINX Ingress**
   - Terminate TLS at ingress layer
   - Use Web Application Firewall (WAF)


2. **Implement Network Policies**
   - Use Calico or Azure Network Policies
   - Restrict pod-to-pod communication


3. **Private AKS Cluster**
   - Use private API server endpoint
   - Access via VPN or Bastion host


4. **Limit public exposure**
   - Use Azure Private Link for ACR
   - Consider internal load balancers for non-public services


### Container Security


**🔒 Best Practices**


1. **Scan images for vulnerabilities**
   - Enable Azure Defender for container registries
   - Use tools like Trivy or Snyk in CI/CD


2. **Use minimal base images**
   - Current Dockerfile uses OpenJDK (consider distroless images)
   - Reduce attack surface by minimizing installed packages


3. **Run as non-root user**
   - Configure Dockerfile with USER directive
   - Set Kubernetes security context


4. **Keep images updated**
   - Regularly rebuild images with latest base image patches
   - Automate vulnerability scanning


### Kubernetes Security


**🔒 Best Practices**


1. **Pod Security Standards**
   - Implement Pod Security Policies or Pod Security Admission
   - Enforce restricted security contexts


2. **Resource Limits**
   - Define CPU and memory limits for pods
   - Prevents resource exhaustion attacks


3. **Secrets Management**
   - Use Azure Key Vault for sensitive data
   - Consider Azure Key Vault Secrets Provider for AKS


4. **Enable audit logging**
   - Monitor AKS control plane logs
   - Send logs to Azure Monitor or Log Analytics


### Terraform Security


**🔒 Best Practices**


1. **Protect state files**
   - Enable encryption at rest for Azure Storage
   - Restrict access using Azure RBAC


2. **Use remote state locking**
   - Prevents concurrent modifications
   - Already implemented with Azure Storage backend


3. **Review plans before apply**
   - Always run `terraform plan` first
   - Use GitHub Environment protection rules for approvals


4. **Sensitive data handling**
   - Never commit `.tfvars` files with secrets to Git
   - Use GitHub Secrets for sensitive values


### GitHub Actions Security


**✅ Current Implementation**
- Uses OIDC (no static credentials)
- Secrets stored in GitHub Secrets (encrypted)
- Workflows require manual approval via Environment


**🔒 Additional Recommendations**


1. **Restrict workflow permissions**
   - Use `permissions:` block in workflows
   - Grant only necessary permissions


2. **Pin action versions**
   - Use commit SHAs instead of tags for third-party actions
   - Example: `actions/checkout@<commit-sha>`


3. **Enable branch protection**
   - Require pull request reviews before merging to main
   - Prevent direct pushes to main branch


4. **Audit workflow runs**
   - Review workflow logs regularly
   - Monitor for unexpected changes


## Notes and constraints


* **ACR naming:** ACR registry names must be globally unique across Azure.
* **Public exposure:** The app is exposed via a Kubernetes Service of type `LoadBalancer`, which creates a public endpoint.
* **Learning setup:** This repo is intentionally minimal and uses a single environment (`poc`).
* **State management:** Each Terraform module (ACR and AKS) maintains separate state files for isolation.
* **Image tagging:** Images are tagged with Git SHA for traceability and rollback capabilities.
* **Single region:** All resources are deployed to a single Azure region defined in tfvars.


## Additional Resources


### Official Documentation


**Azure**
- [Azure Kubernetes Service (AKS) Documentation](https://learn.microsoft.com/en-us/azure/aks/)
- [Azure Container Registry Documentation](https://learn.microsoft.com/en-us/azure/container-registry/)
- [Azure OIDC with GitHub Actions](https://learn.microsoft.com/en-us/azure/developer/github/connect-from-azure)


**Terraform**
- [Terraform AzureRM Provider](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)
- [Terraform Kubernetes Provider](https://registry.terraform.io/providers/hashicorp/kubernetes/latest/docs)
- [Terraform Best Practices](https://www.terraform-best-practices.com/)


**Kubernetes**
- [Kubernetes Official Documentation](https://kubernetes.io/docs/home/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Kubernetes Deployment Strategies](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)


**GitHub Actions**
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Workflow Syntax](https://docs.github.com/en/actions/reference/workflow-syntax-for-github-actions)
- [Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)


### Learning Resources


**Tutorials**
- [AKS Tutorial](https://learn.microsoft.com/en-us/azure/aks/tutorial-kubernetes-prepare-app)
- [Terraform Associate Certification](https://www.hashicorp.com/certification/terraform-associate)
- [Kubernetes Basics](https://kubernetes.io/docs/tutorials/kubernetes-basics/)


**Community**
- [Terraform Azure Examples](https://github.com/hashicorp/terraform-provider-azurerm/tree/main/examples)
- [AKS Baseline Architecture](https://learn.microsoft.com/en-us/azure/architecture/reference-architectures/containers/aks/baseline-aks)
- [Kubernetes Community](https://kubernetes.io/community/)


### Related Tools


- **Azure CLI**: [Installation Guide](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)
- **kubectl**: [Installation Guide](https://kubernetes.io/docs/tasks/tools/)
- **Docker Desktop**: [Download](https://www.docker.com/products/docker-desktop/)
- **Terraform**: [Download](https://www.terraform.io/downloads)
- **k9s**: Terminal UI for Kubernetes - [GitHub](https://github.com/derailed/k9s)
- **Lens**: Kubernetes IDE - [Website](https://k8slens.dev/)


### Example Enhancements


Once you're comfortable with this basic setup, consider these next steps:


1. **Add monitoring and observability**
   - Integrate Azure Monitor for containers
   - Set up Prometheus and Grafana
   - Configure Application Insights


2. **Implement ingress controller**
   - Deploy NGINX Ingress Controller
   - Configure TLS/SSL certificates with cert-manager
   - Use Azure Application Gateway Ingress Controller


3. **Add database layer**
   - Deploy Azure Database for PostgreSQL/MySQL
   - Use Kubernetes StatefulSets for databases
   - Configure persistent volumes


4. **Implement GitOps**
   - Use Flux or ArgoCD for continuous deployment
   - Automate synchronization from Git to cluster


5. **Multi-environment setup**
   - Create dev, staging, and production environments
   - Use Terraform workspaces or separate tfvars
   - Implement environment-specific configurations


6. **Advanced networking**
   - Configure Azure CNI networking
   - Implement Network Policies
   - Set up Azure Firewall


7. **CI/CD improvements**
   - Add automated testing (unit, integration, e2e)
   - Implement security scanning in pipeline
   - Use matrix strategies for multi-environment deployments


8. **Helm charts**
   - Package applications using Helm
   - Version and manage Kubernetes manifests
   - Use Helm for application configuration


---


**Made with ☁️ for learning Azure, Kubernetes, and Terraform**


For questions, issues, or contributions, please open an issue in this repository.