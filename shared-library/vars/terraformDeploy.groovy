// Terraform init → validate → plan → approve → apply.

def call(Map config = [:]) {
    def environment = config.environment ?: error('environment must be set')
    def target      = config.target      ?: error('target must be set')
    def credId      = params.CREDENTIAL_ID?.trim() ?: "aws-terraform-cred-${environment}"
    def awsRegion   = params.AWS_REGION?.trim()    ?: 'us-west-1'
    def tfDir       = "environments/${environment}/${target}"

    stage("Checkout managed-aws-terraform") {
        checkout([
            $class: 'GitSCM',
            branches: [[name: '*/main']],
            userRemoteConfigs: [[
                url: 'YOUR_GIT_REPO_URL/managed-aws-terraform.git',
                credentialsId: 'git-cred'
            ]]
        ])
    }

    stage("Terraform Init (${environment}/${target})") {
        dir(tfDir) {
            withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                              credentialsId: credId,
                              accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                              secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                withEnv(["AWS_DEFAULT_REGION=${awsRegion}"]) {
                    sh 'terraform init -reconfigure'
                }
            }
        }
    }

    stage('Terraform Validate') {
        dir(tfDir) { sh 'terraform validate' }
    }

    stage("Terraform Plan (${environment}/${target})") {
        dir(tfDir) {
            withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                              credentialsId: credId,
                              accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                              secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                withEnv(["AWS_DEFAULT_REGION=${awsRegion}"]) {
                    sh 'terraform plan -var-file=terraform.tfvars -out=tfplan'
                }
            }
        }
    }

    stage('Approval') {
        timeout(time: 30, unit: 'MINUTES') {
            input(message: "Apply Terraform plan for ${environment}/${target}?", ok: 'Apply')
        }
    }

    stage("Terraform Apply (${environment}/${target})") {
        dir(tfDir) {
            withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                              credentialsId: credId,
                              accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                              secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                withEnv(["AWS_DEFAULT_REGION=${awsRegion}"]) {
                    sh 'terraform apply tfplan'
                }
            }
        }
    }
}
