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
                sh 'docker build --no-cache -t $IMAGE_NAME .'
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
                sh 'sudo /usr/local/bin/deploy-salescrm'
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