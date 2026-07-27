// Build Docker image, Trivy scan, push to dev ECR, optionally deploy to dev EKS.

def call(String serviceName, Map svcCfg, Map envCfg, String ecrImage) {
    def branch      = params.BRANCH ?: 'main'
    def imageTag    = "master-${env.BUILD_NUMBER}"
    def fullImage   = "${ecrImage}:${imageTag}"
    def awsRegion   = envCfg.awsRegion    ?: 'us-west-1'
    def awsAccountId = envCfg.awsAccountId ?: ''
    def ecrHost     = "${awsAccountId}.dkr.ecr.${awsRegion}.amazonaws.com"
    def credId      = envCfg.awsCredId    ?: 'aws-dev-ecr-cred'
    def dockerfile  = svcCfg.dockerfile   ?: 'Dockerfile'
    def context     = svcCfg.dockerContext ?: '.'

    stage('Checkout') {
        checkout([$class: 'GitSCM',
                  branches: [[name: "*/${branch}"]],
                  userRemoteConfigs: scm.userRemoteConfigs])
    }

    stage('Build Docker Image') {
        sh "docker build -t ${fullImage} -f ${dockerfile} ${context}"
    }

    stage('Trivy Scan') {
        sh """
            trivy image --exit-code 1 --severity HIGH,CRITICAL \\
                --format table ${fullImage}
        """
    }

    stage('Push to Dev ECR') {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                          credentialsId: credId,
                          accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                          secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            sh """
                aws ecr get-login-password --region ${awsRegion} | \\
                    docker login --username AWS --password-stdin ${ecrHost}
                docker push ${fullImage}
            """
        }
    }

    if (envCfg.autoDeploy) {
        stage('Deploy to Dev EKS') {
            deployEks(
                serviceName:  serviceName,
                namespace:    envCfg.namespace    ?: 'margbooks-dev',
                ecrImage:     ecrImage,
                imageTag:     imageTag,
                kubeconfigId: envCfg.kubeconfigId ?: 'eks-dev-kubeconfig',
                chart:        envCfg.helmChart    ?: 'helm/charts/api',
                valuesFile:   "${envCfg.helmChart ?: 'helm/charts/api'}/values-dev.yaml"
            )
        }
    }

    echo "Dev build complete: ${fullImage}"
}
