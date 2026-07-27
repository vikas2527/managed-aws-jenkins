// Deploy or upgrade a Helm release on EKS.
// Checks out managed-aws-jenkins repo for Helm charts.

def call(Map config = [:]) {
    def serviceName  = config.serviceName  ?: error('serviceName required')
    def namespace    = config.namespace    ?: error('namespace required')
    def ecrImage     = config.ecrImage     ?: error('ecrImage required')
    def imageTag     = config.imageTag     ?: error('imageTag required')
    def kubeconfigId = config.kubeconfigId ?: 'eks-dev-kubeconfig'
    def chart        = config.chart        ?: 'helm/charts/api'
    def valuesFile   = config.valuesFile   ?: "${chart}/values-dev.yaml"
    def timeout      = config.timeout      ?: '5m'

    // Checkout managed-aws-jenkins to get Helm charts
    dir('jenkins-repo') {
        checkout([
            $class: 'GitSCM',
            branches: [[name: '*/main']],
            userRemoteConfigs: [[
                url: 'YOUR_GIT_REPO_URL/managed-aws-jenkins.git',
                credentialsId: 'git-cred'
            ]]
        ])
    }

    withCredentials([file(credentialsId: kubeconfigId, variable: 'KUBECONFIG')]) {
        sh """
            helm upgrade --install ${serviceName} ${WORKSPACE}/jenkins-repo/${chart} \\
                -f ${WORKSPACE}/jenkins-repo/${valuesFile} \\
                --set image.repository=${ecrImage} \\
                --set image.tag=${imageTag} \\
                --namespace ${namespace} \\
                --create-namespace \\
                --wait --timeout=${timeout}
        """

        sh """
            kubectl rollout status deployment/${serviceName} -n ${namespace} --timeout=3m
            kubectl get pods -n ${namespace} -l app=${serviceName}
        """
    }
}
