// Jenkins Job DSL — SonarQube scan jobs.

def JENKINS_REPO_URL = 'https://github.com/vikas2527/managed-aws-jenkins.git'
def GIT_CRED_ID      = 'git-cred'

pipelineJob('sonarqube/scan-all-services') {
    description('Run SonarQube scan across all services.')
    properties { disableConcurrentBuilds() }

    parameters {
        stringParam('BRANCH', 'main', 'Branch to scan')
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote { url(JENKINS_REPO_URL); credentials(GIT_CRED_ID) }
                    branch('main')
                }
            }
            scriptPath('Jenkinsfile.sonarqube-scan-all')
            lightweight(true)
        }
    }
}
