// Jenkins Job DSL — Terraform apply + destroy jobs for all environments.

def JENKINS_REPO_URL = 'https://github.com/vikas2527/managed-aws-jenkins.git'
def GIT_CRED_ID      = 'git-cred'

def jobs = [
    // Dev
    [folder: 'dev-infra-creation',    environment: 'dev',    target: 'vpc',         credentialId: 'aws-terraform-cred-dev'],
    [folder: 'dev-infra-creation',    environment: 'dev',    target: 'eks',         credentialId: 'aws-terraform-cred-dev'],
    [folder: 'dev-infra-creation',    environment: 'dev',    target: 'rds',         credentialId: 'aws-terraform-cred-dev'],
    // Prod
    [folder: 'prod-infra-creation',   environment: 'prod',   target: 'vpc',         credentialId: 'aws-terraform-cred-prod'],
    [folder: 'prod-infra-creation',   environment: 'prod',   target: 'eks',         credentialId: 'aws-terraform-cred-prod'],
    [folder: 'prod-infra-creation',   environment: 'prod',   target: 'rds',         credentialId: 'aws-terraform-cred-prod'],
    [folder: 'prod-infra-creation',   environment: 'prod',   target: 'alb',         credentialId: 'aws-terraform-cred-prod'],
    // Shared
    [folder: 'shared-infra-creation', environment: 'shared', target: 'vpc',         credentialId: 'aws-terraform-cred-shared'],
    [folder: 'shared-infra-creation', environment: 'shared', target: 'ecr',         credentialId: 'aws-terraform-cred-shared'],
    [folder: 'shared-infra-creation', environment: 'shared', target: 'jenkins',     credentialId: 'aws-terraform-cred-shared'],
    [folder: 'shared-infra-creation', environment: 'shared', target: 'devtools',    credentialId: 'aws-terraform-cred-shared'],
    [folder: 'shared-infra-creation', environment: 'shared', target: 'vpc-peering', credentialId: 'aws-terraform-cred-shared'],
    [folder: 'shared-infra-creation', environment: 'shared', target: 'vpn',         credentialId: 'aws-terraform-cred-shared'],
]

jobs.each { cfg ->
    pipelineJob("${cfg.folder}/terraform-apply-${cfg.target}") {
        description("Terraform plan → approve → apply for ${cfg.environment}/${cfg.target}")
        properties { disableConcurrentBuilds() }

        parameters {
            stringParam('AWS_REGION',    'us-west-1',      'AWS region')
            stringParam('CREDENTIAL_ID', cfg.credentialId, 'Jenkins AWS credential ID')
        }

        definition {
            cpsScm {
                scm {
                    git {
                        remote { url(JENKINS_REPO_URL); credentials(GIT_CRED_ID) }
                        branch('main')
                    }
                }
                scriptPath("Jenkinsfile.terraform.${cfg.environment}")
                lightweight(true)
            }
        }
    }

    pipelineJob("${cfg.folder}/terraform-destroy-${cfg.target}") {
        description("Terraform DESTROY for ${cfg.environment}/${cfg.target}. USE WITH CAUTION.")
        properties { disableConcurrentBuilds() }

        parameters {
            stringParam('AWS_REGION',    'us-west-1',      'AWS region')
            stringParam('CREDENTIAL_ID', cfg.credentialId, 'Jenkins AWS credential ID')
        }

        definition {
            cpsScm {
                scm {
                    git {
                        remote { url(JENKINS_REPO_URL); credentials(GIT_CRED_ID) }
                        branch('main')
                    }
                }
                scriptPath("Jenkinsfile.terraform.destroy.${cfg.environment}")
                lightweight(true)
            }
        }
    }
}
