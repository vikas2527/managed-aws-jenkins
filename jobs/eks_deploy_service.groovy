// Jenkins Job DSL — App CI/CD pipelines for all services.

def JENKINS_REPO_URL  = 'YOUR_GIT_REPO_URL/managed-aws-jenkins.git'
def API_REPO_URL      = 'YOUR_GIT_REPO_URL/margbooks-api.git'
def UI_REPO_URL       = 'YOUR_GIT_REPO_URL/margbooks-ui.git'
def GIT_CRED_ID       = 'git-cred'

def apiServices = [
    'catalog-api',
    'inventory-api',
    'customer-api',
    'order-api',
    'notification-api'
]

// ── Dev pipelines for API services ───────────────────────────────────────────
apiServices.each { svc ->
    pipelineJob("dev/${svc}") {
        description("Dev CI/CD pipeline for ${svc} — build, scan, push, deploy to dev EKS")
        properties { disableConcurrentBuilds() }

        triggers {
            scm('H/5 * * * *')   // poll SCM every 5 minutes
        }

        definition {
            cpsScm {
                scm {
                    git {
                        remote { url(API_REPO_URL); credentials(GIT_CRED_ID) }
                        branch('main')
                    }
                }
                scriptPath('Jenkinsfile')
                lightweight(true)
            }
        }
    }

    pipelineJob("prod/${svc}") {
        description("Prod pipeline for ${svc} — promote image to prod ECR, approve, deploy to prod EKS")
        properties { disableConcurrentBuilds() }

        parameters {
            stringParam('IMAGE_TAG', '', "Image tag from dev ECR to promote — must start with 'master-'")
        }

        definition {
            cpsScm {
                scm {
                    git {
                        remote { url(API_REPO_URL); credentials(GIT_CRED_ID) }
                        branch('main')
                    }
                }
                scriptPath('Jenkinsfile')
                lightweight(true)
            }
        }
    }
}

// ── Dev pipeline for UI ───────────────────────────────────────────────────────
pipelineJob('dev/ui') {
    description('Dev CI/CD pipeline for margbooks-ui — build Angular, push to ECR, deploy to dev EKS')
    properties { disableConcurrentBuilds() }

    triggers {
        scm('H/5 * * * *')
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote { url(UI_REPO_URL); credentials(GIT_CRED_ID) }
                    branch('main')
                }
            }
            scriptPath('Jenkinsfile')
            lightweight(true)
        }
    }
}

pipelineJob('prod/ui') {
    description('Prod pipeline for margbooks-ui — promote image, approve, deploy to prod EKS')
    properties { disableConcurrentBuilds() }

    parameters {
        stringParam('IMAGE_TAG', '', "Image tag from dev ECR to promote — must start with 'master-'")
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote { url(UI_REPO_URL); credentials(GIT_CRED_ID) }
                    branch('main')
                }
            }
            scriptPath('Jenkinsfile')
            lightweight(true)
        }
    }
}

// ── Manual deploy job ─────────────────────────────────────────────────────────
['dev', 'prod'].each { environment ->
    pipelineJob("${environment}-operations/eks-deploy-service") {
        description("Manually deploy any service to ${environment} EKS cluster.")
        properties { disableConcurrentBuilds() }

        parameters {
            stringParam('SERVICE_NAME',  '',                          'Helm release / deployment name')
            stringParam('NAMESPACE',     "margbooks-${environment}",  'Kubernetes namespace')
            stringParam('IMAGE',         '',                          'Full ECR image URI')
            stringParam('IMAGE_TAG',     '',                          'Image tag to deploy')
            stringParam('KUBECONFIG_ID', "eks-${environment}-kubeconfig", 'Jenkins credential ID for kubeconfig')
            choiceParam('CHART',         ['api', 'ui'],               'Which Helm chart to use')
        }

        definition {
            cpsScm {
                scm {
                    git {
                        remote { url(JENKINS_REPO_URL); credentials(GIT_CRED_ID) }
                        branch('main')
                    }
                }
                scriptPath('Jenkinsfile.deploy-eks')
                lightweight(true)
            }
        }
    }
}
