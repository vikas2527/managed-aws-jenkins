// Jenkins Job DSL — Promote image from dev ECR to prod ECR.

def JENKINS_REPO_URL = 'https://github.com/vikas2527/managed-aws-jenkins.git'
def GIT_CRED_ID      = 'git-cred'

pipelineJob('prod-operations/promote-image-to-prod-ecr') {
    description('Pull a master- image from dev ECR, retag as prod-, push to prod ECR.')
    properties { disableConcurrentBuilds() }

    parameters {
        stringParam('SERVICE_NAME',     '',                   'ECR repository / service name (e.g. margbooks/catalog-api)')
        stringParam('SOURCE_IMAGE_TAG', '',                   "Tag in dev ECR — must start with 'master-'")
        stringParam('DEV_AWS_CRED',     'aws-dev-ecr-cred',  'Jenkins credential for dev ECR')
        stringParam('PROD_AWS_CRED',    'aws-prod-ecr-cred', 'Jenkins credential for prod ECR')
        stringParam('AWS_REGION',       'us-west-1',         'AWS region')
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote { url(JENKINS_REPO_URL); credentials(GIT_CRED_ID) }
                    branch('main')
                }
            }
            scriptPath('Jenkinsfile.promote-image-to-prod-ecr')
            lightweight(true)
        }
    }
}
