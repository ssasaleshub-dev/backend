pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    triggers {
        githubPush()
    }

    environment {
        IMAGE_NAME = 'ssasaleshub/backend:latest'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    docker build --no-cache -t $IMAGE_NAME .
                    docker tag $IMAGE_NAME salescrm-platform-salescrm-app:latest
                    docker tag $IMAGE_NAME salescrm-platform-salescrm-app
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
                        echo "$DOCKER_TOKEN" | docker login \
                            -u "$DOCKER_USER" --password-stdin
                        docker push "$IMAGE_NAME"
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    echo "Checking /usr/local/bin/deploy-salescrm script:"
                    cat /usr/local/bin/deploy-salescrm || true

                    echo "Stopping and removing stale salescrm-app container..."
                    docker stop salescrm-app 2>/dev/null || true
                    docker rm salescrm-app 2>/dev/null || true
                    docker stop salescrm-platform-salescrm-app-1 2>/dev/null || true
                    docker rm salescrm-platform-salescrm-app-1 2>/dev/null || true

                    echo "Starting fresh deployment..."
                    sudo /usr/local/bin/deploy-salescrm
                '''
            }
        }
    }

    post {
        success {
            echo 'SalesCRM deployed successfully!'
        }
        failure {
            echo 'Pipeline failed. Check the stage logs.'
        }
    }
}