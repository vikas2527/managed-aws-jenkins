// Run a SonarQube scan for a single service.

def call(String serviceName, Map svc, Map sonarCfg) {
    def sonarUrl    = sonarCfg.url    ?: env.SONAR_HOST_URL
    def sonarCredId = sonarCfg.credId ?: 'sonarqube-token'
    def projectKey  = svc.sonarProjectKey ?: serviceName

    stage("Sonar: ${serviceName}") {
        withCredentials([string(credentialsId: sonarCredId, variable: 'SONAR_TOKEN')]) {
            sh """
                sonar-scanner \\
                    -Dsonar.projectKey=${projectKey} \\
                    -Dsonar.host.url=${sonarUrl} \\
                    -Dsonar.login=\${SONAR_TOKEN} \\
                    -Dsonar.sources=.
            """
        }
    }
}
