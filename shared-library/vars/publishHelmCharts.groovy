// Package and publish Helm charts to S3 as a Helm repository.

def call(Map config = [:]) {
    def chartPath = config.chartPath ?: 'helm/charts/api'
    def s3Bucket  = config.s3Bucket  ?: error('s3Bucket required')
    def s3Prefix  = config.s3Prefix  ?: 'helm-charts'
    def awsRegion = config.awsRegion ?: 'us-west-1'
    def credId    = config.credId    ?: 'aws-helm-publish-cred'

    stage('Package Helm Chart') {
        sh "helm package ${chartPath} --destination .helm-pkg/"
    }

    stage('Publish to S3') {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                          credentialsId: credId,
                          accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                          secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            withEnv(["AWS_DEFAULT_REGION=${awsRegion}"]) {
                sh """
                    aws s3 sync .helm-pkg/ s3://${s3Bucket}/${s3Prefix}/ --delete
                    helm repo index .helm-pkg/ --url https://${s3Bucket}.s3.${awsRegion}.amazonaws.com/${s3Prefix}
                    aws s3 cp .helm-pkg/index.yaml s3://${s3Bucket}/${s3Prefix}/index.yaml
                """
            }
        }
    }
}
