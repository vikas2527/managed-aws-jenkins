// Promote dev image to prod ECR and deploy to prod EKS with manual approval gate.

def call(String serviceName, Map svcCfg, Map envCfg, String ecrImage) {
    def imageTag = params.IMAGE_TAG?.trim()
    if (!imageTag) { error('IMAGE_TAG must be set for prod promotion') }
    if (!imageTag.startsWith('master-')) {
        error("IMAGE_TAG must start with 'master-', got: ${imageTag}")
    }

    def prodTag = imageTag.replace('master-', 'prod-')
    def credId  = envCfg.awsCredId ?: 'aws-prod-ecr-cred'

    stage('Promote Image to Prod ECR') {
        promoteImageToEcr(
            serviceName: "${envCfg.ecrPrefix}/${serviceName}",
            sourceTag:   imageTag,
            targetTag:   prodTag,
            devCredId:   'aws-dev-ecr-cred',
            prodCredId:  credId,
            awsRegion:   envCfg.awsRegion ?: 'us-west-1'
        )
    }

    stage('Approval') {
        timeout(time: 30, unit: 'MINUTES') {
            input(message: "Deploy ${serviceName}:${prodTag} to PRODUCTION?", ok: 'Deploy')
        }
    }

    stage('Deploy to Prod EKS') {
        deployEks(
            serviceName:  serviceName,
            namespace:    envCfg.namespace    ?: 'margbooks-prod',
            ecrImage:     ecrImage,
            imageTag:     prodTag,
            kubeconfigId: envCfg.kubeconfigId ?: 'eks-prod-kubeconfig',
            chart:        envCfg.helmChart    ?: 'helm/charts/api',
            valuesFile:   "${envCfg.helmChart ?: 'helm/charts/api'}/values-prod.yaml"
        )
    }

    echo "Prod deploy complete: ${ecrImage}:${prodTag}"
}
