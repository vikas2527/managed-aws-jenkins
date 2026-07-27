# managed-aws-jenkins

Jenkins configuration-as-code for the managed AWS platform — pipelines, Job DSL seed scripts, and the shared pipeline library.

## Structure

```
managed-aws-jenkins/
├── Jenkinsfile                              # App CI/CD entry point (calls awsPipeline())
├── Jenkinsfile.deploy-eks                  # Deploy a Helm chart to EKS (manual trigger)
├── Jenkinsfile.nightly                     # Nightly build + Trivy scan + optional deploy
├── Jenkinsfile.promote-image-to-prod-ecr  # Promote dev image to prod ECR
├── Jenkinsfile.scale-up                   # Scale up EKS node group
├── Jenkinsfile.scale-down                 # Scale down EKS node group
├── Jenkinsfile.sonarqube-scan-all         # SonarQube scan across all services
├── Jenkinsfile.terraform.dev              # Terraform apply for dev (vpc/eks/rds)
├── Jenkinsfile.terraform.prod             # Terraform apply for prod (vpc/eks/rds/alb)
├── Jenkinsfile.terraform.shared           # Terraform apply for shared (ecr/jenkins/devtools/vpn/vpc-peering)
├── Jenkinsfile.terraform.destroy.dev      # Terraform destroy for dev
├── Jenkinsfile.terraform.destroy.prod     # Terraform destroy for prod
├── jobs/
│   ├── folders.groovy                     # Creates all Jenkins folder structure
│   ├── views.groovy                       # List views grouping jobs by environment
│   ├── terraform.groovy                   # Seed: terraform apply+destroy jobs for all environments
│   ├── eks_deploy_service.groovy          # Seed: Helm deploy jobs for dev + prod
│   ├── promote_image_to_prod_ecr.groovy   # Seed: ECR promotion job
│   └── sonarqube.groovy                   # Seed: SonarQube scan job
├── resources/                             # K8s resource YAMLs applied via Jenkins
│   ├── aws-ebs-sc-prod.yaml
│   ├── grafana-dev-values.example.yaml
│   ├── grafana-prod-values.example.yaml
│   ├── nginx-ingress-prod-values.yaml
│   └── prometheus-prod-values.yaml
└── shared-library/
    └── vars/
        ├── awsPipeline.groovy             # Router: reads jenkins-service-registry.yaml
        ├── runDev.groovy                  # Build → Trivy → push to dev ECR → deploy to dev EKS
        ├── runProd.groovy                 # Promote → approval → deploy to prod EKS
        ├── deployEks.groovy               # Helm upgrade/install on EKS
        ├── terraformDeploy.groovy         # terraform init → validate → plan → approve → apply
        ├── terraformDestroy.groovy        # terraform destroy with approval gate
        ├── promoteImageToEcr.groovy       # Pull dev ECR image, retag, push to prod ECR
        ├── publishHelmCharts.groovy       # Package + publish Helm charts to S3
        └── runSonar.groovy                # SonarQube scan for a single service
```

## Jenkins shared library setup

Configure this repo as a Global Shared Library in **Manage Jenkins → Configure System → Global Pipeline Libraries**:

- **Name**: `managed-aws`
- **Default version**: `main`
- **Retrieval method**: Modern SCM → Git → this repo URL
- **Library path**: `shared-library`

## Jenkins credentials required

| Credential ID | Type | Used for |
|---|---|---|
| `git-cred` | Username/password or SSH key | Git checkouts |
| `aws-terraform-cred-dev` | AWS credentials | Terraform dev |
| `aws-terraform-cred-prod` | AWS credentials | Terraform prod |
| `aws-terraform-cred-shared` | AWS credentials | Terraform shared |
| `aws-dev-ecr-cred` | AWS credentials | Dev ECR push/pull |
| `aws-prod-ecr-cred` | AWS credentials | Prod ECR push/pull |
| `eks-dev-kubeconfig` | Secret file | kubectl/helm on dev EKS |
| `eks-prod-kubeconfig` | Secret file | kubectl/helm on prod EKS |
| `sonarqube-token` | Secret text | SonarQube scans |

## Seed job

Run the seed job pointing at `jobs/folders.groovy` first, then `jobs/terraform.groovy`, `jobs/eks_deploy_service.groovy`, etc. to auto-create all pipeline jobs and folder structure.

## jenkins-service-registry.yaml

Each application repo must contain a `jenkins-service-registry.yaml` at its root. Example:

```yaml
defaults:
  sonarProjectKey: my-service

environments:
  dev:
    awsRegion:    us-east-1
    awsAccountId: "111111111111"
    awsCredId:    aws-dev-ecr-cred
    ecrHost:      111111111111.dkr.ecr.us-east-1.amazonaws.com
    ecrPrefix:    my-project
    kubeconfigId: eks-dev-kubeconfig
    helmChart:    charts/app
    autoDeploy:   true

  prod:
    awsRegion:    us-east-1
    awsAccountId: "222222222222"
    awsCredId:    aws-prod-ecr-cred
    ecrHost:      222222222222.dkr.ecr.us-east-1.amazonaws.com
    ecrPrefix:    my-project
    kubeconfigId: eks-prod-kubeconfig
    helmChart:    charts/app

sonarqube:
  url:    http://sonarqube.internal
  credId: sonarqube-token

services:
  my-service:
    sonarProjectKey: my-service
```
