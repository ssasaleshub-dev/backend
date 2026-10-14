
pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        githubPush()
    }

    environment {
        IMAGE_NAME = 'ssasaleshub/backend'
        DEPLOY_SCRIPT = '/usr/local/bin/deploy-salescrm'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build JAR') {
            steps {
                sh '''
                    set -eu
                    chmod +x mvnw
                    ./mvnw -B clean package
                    echo "Generated JAR files:"
                    find target -maxdepth 1 -type f -name "*.jar" -print
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -eu
                    test -f Dockerfile
                    docker build \
                        -t "$IMAGE_NAME:$BUILD_NUMBER" \
                        -t "$IMAGE_NAME:latest" \
                        .
                '''
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'ssasaleshub_docker',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_TOKEN'
                    )
                ]) {
                    sh '''
                        set +x
                        set -eu

                        echo "$DOCKER_TOKEN" | docker login \
                            --username "$DOCKER_USER" \
                            --password-stdin

                        docker push "$IMAGE_NAME:$BUILD_NUMBER"
                        docker push "$IMAGE_NAME:latest"

                        docker logout
                    '''
                }
            }
        }

        stage('Deploy to Hostinger VPS') {
            steps {
                sh '''
                    set -eu

                    test -x "$DEPLOY_SCRIPT"
                    sudo "$DEPLOY_SCRIPT"
                '''
            }
        }
    }

    post {
        success {
            echo 'SalesCRM JAR built, Docker image pushed, and deployment completed.'
        }

        failure {
            echo 'Pipeline failed. Check the failed stage logs.'
        }

        always {
            echo 'SalesCRM pipeline finished.'
        }
    }
}
