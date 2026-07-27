// Pull image from dev ECR, retag, push to prod ECR.

def call(Map config = [:]) {
    def serviceName = config.serviceName ?: error('serviceName required')
    def sourceTag   = config.sourceTag   ?: error('sourceTag required')
    def targetTag   = config.targetTag   ?: error('targetTag required')
    def devCredId   = config.devCredId   ?: 'aws-dev-ecr-cred'
    def prodCredId  = config.prodCredId  ?: 'aws-prod-ecr-cred'
    def awsRegion   = config.awsRegion   ?: 'us-west-1'

    withCredentials([
        [$class: 'AmazonWebServicesCredentialsBinding',
         credentialsId: devCredId,
         accessKeyVariable: 'DEV_AWS_ACCESS_KEY_ID',
         secretKeyVariable: 'DEV_AWS_SECRET_ACCESS_KEY'],
        [$class: 'AmazonWebServicesCredentialsBinding',
         credentialsId: prodCredId,
         accessKeyVariable: 'PROD_AWS_ACCESS_KEY_ID',
         secretKeyVariable: 'PROD_AWS_SECRET_ACCESS_KEY']
    ]) {
        withEnv(["AWS_DEFAULT_REGION=${awsRegion}"]) {
            sh """
                # Get dev ECR account ID and pull image
                export AWS_ACCESS_KEY_ID=\$DEV_AWS_ACCESS_KEY_ID
                export AWS_SECRET_ACCESS_KEY=\$DEV_AWS_SECRET_ACCESS_KEY
                DEV_ACCOUNT=\$(aws sts get-caller-identity --query Account --output text)
                DEV_ECR=\${DEV_ACCOUNT}.dkr.ecr.${awsRegion}.amazonaws.com

                aws ecr get-login-password --region ${awsRegion} | \\
                    docker login --username AWS --password-stdin \${DEV_ECR}
                docker pull \${DEV_ECR}/${serviceName}:${sourceTag}

                # Get prod ECR account ID and push image
                export AWS_ACCESS_KEY_ID=\$PROD_AWS_ACCESS_KEY_ID
                export AWS_SECRET_ACCESS_KEY=\$PROD_AWS_SECRET_ACCESS_KEY
                PROD_ACCOUNT=\$(aws sts get-caller-identity --query Account --output text)
                PROD_ECR=\${PROD_ACCOUNT}.dkr.ecr.${awsRegion}.amazonaws.com

                docker tag \${DEV_ECR}/${serviceName}:${sourceTag} \${PROD_ECR}/${serviceName}:${targetTag}
                aws ecr get-login-password --region ${awsRegion} | \\
                    docker login --username AWS --password-stdin \${PROD_ECR}
                docker push \${PROD_ECR}/${serviceName}:${targetTag}

                echo "Promoted: \${DEV_ECR}/${serviceName}:${sourceTag} → \${PROD_ECR}/${serviceName}:${targetTag}"
            """
        }
    }
}
