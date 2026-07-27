// AWS EKS application pipeline — single entry point for all service jobs.
// JOB_NAME format: {Env}/{service}
//   Env: dev | prod | sonarqube

def call() {
    def parts = env.JOB_NAME.tokenize('/')
    if (parts.size() < 2) {
        error("JOB_NAME '${env.JOB_NAME}' must be in format Env/service")
    }
    def envName     = parts[0].toLowerCase()
    def serviceName = parts[1]

    if (envName == 'dev') {
        def scmBranch = scm.branches[0].name.replaceFirst(/^\*\//, '')
        properties([
            disableConcurrentBuilds(),
            parameters([
                string(name: 'BRANCH', defaultValue: scmBranch,
                       description: 'Git branch to build and push to dev ECR')
            ])
        ])
    } else if (envName == 'prod') {
        properties([
            disableConcurrentBuilds(),
            parameters([
                string(name: 'IMAGE_TAG', defaultValue: '',
                       description: "Image tag from dev ECR to promote — must start with 'master-'")
            ])
        ])
    } else {
        properties([disableConcurrentBuilds()])
    }

    node {
        try {
            checkout([
                $class: 'GitSCM',
                branches: scm.branches,
                extensions: [
                    [$class: 'CloneOption', shallow: true, depth: 1, noTags: true]
                ],
                userRemoteConfigs: scm.userRemoteConfigs
            ])

            def registry = readYaml file: 'jenkins-service-registry.yaml'

            if (envName == 'sonarqube') {
                def svc = [:] + (registry.defaults ?: [:]) + ((registry.services ?: [:])[serviceName] ?: [:])
                runSonar(serviceName, svc, registry.sonarqube ?: [:])
                return
            }

            def envCfg = registry.environments?.get(envName)
            if (!envCfg) {
                error("No environment config for '${envName}' in jenkins-service-registry.yaml")
            }

            def svcCfg   = (registry.services ?: [:])[serviceName] ?: [:]
            def ecrHost  = envCfg.ecrHost
            def ecrRepo  = "${envCfg.ecrPrefix}/${serviceName}"
            def ecrImage = "${ecrHost}/${ecrRepo}"

            switch (envName) {
                case 'dev':
                    runDev(serviceName, svcCfg, envCfg, ecrImage)
                    break
                case 'prod':
                    runProd(serviceName, svcCfg, envCfg, ecrImage)
                    break
                default:
                    error("Unknown environment '${envName}'")
            }
        } finally {
            cleanWs()
        }
    }
}
