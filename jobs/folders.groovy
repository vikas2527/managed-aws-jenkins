// Jenkins Job DSL — Folder definitions

folder('dev-infra-creation') {
    description('Dev environment — Terraform infrastructure pipelines (VPC, EKS, RDS)')
}

folder('prod-infra-creation') {
    description('Production environment — Terraform infrastructure pipelines (VPC, EKS, RDS, ALB)')
}

folder('shared-infra-creation') {
    description('Shared infra — Jenkins, devtools, ECR, VPN, VPC peering')
}

folder('dev-operations') {
    description('Dev day-to-day operations — Helm deploy, scale, image builds')
}

folder('prod-operations') {
    description('Prod day-to-day operations — Helm deploy, promote, scale')
}

folder('sonarqube') {
    description('SonarQube scan jobs for all services')
}

// App service folders
folder('dev') {
    description('Dev application pipelines')
}

folder('prod') {
    description('Prod application pipelines')
}
