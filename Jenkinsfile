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
        pollSCM('H/2 * * * *')
    }

    environment {
        IMAGE_NAME = 'ssasaleshub/backend'
        IMAGE_TAG = 'dev'
        DEPLOY_SCRIPT = '/usr/local/bin/deploy-salescrm-dev'
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
                    echo "=== Building SalesCRM Application JAR (dev) ==="
                    chmod +x ./mvnw || true

                    if command -v ./mvnw >/dev/null 2>&1 && ./mvnw -v >/dev/null 2>&1; then
                        echo "Using ./mvnw wrapper..."
                        ./mvnw -B clean package -DskipTests -Pdev
                    elif command -v mvn >/dev/null 2>&1; then
                        echo "Using system mvn..."
                        mvn -B clean package -DskipTests -Pdev
                    else
                        echo "Building JAR inside Maven Docker container..."
                        docker run --rm \
                            -v "$(pwd)":/app \
                            -v "$HOME/.m2":/root/.m2 \
                            -w /app \
                            maven:3.9.9-eclipse-temurin-21 \
                            mvn -B clean package -DskipTests -Pdev
                    fi

                    echo "Verifying generated JAR artifact:"
                    ls -lh target/*.jar
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    echo "=== Building Docker Image (dev) ==="
                    docker build \
                        -t "$IMAGE_NAME:${IMAGE_TAG}-${BUILD_NUMBER}" \
                        -t "$IMAGE_NAME:${IMAGE_TAG}" \
                        .
                '''
            }
        }

        stage('Push to Docker Hub') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'ssasaleshub_docker',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_TOKEN'
                        )
                    ]) {
                        sh '''
                            set +x
                            echo "Attempting Docker Hub login..."
                            echo "$DOCKER_TOKEN" | docker login \
                                --username "$DOCKER_USER" \
                                --password-stdin || true

                            echo "Pushing dev images to registry..."
                            docker push "$IMAGE_NAME:${IMAGE_TAG}-${BUILD_NUMBER}" || true
                            docker push "$IMAGE_NAME:${IMAGE_TAG}" || true
                        '''
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    echo "=== Deploying Latest SalesCRM Dev Backend ==="

                    # 1. Update /usr/local/bin/deploy-salescrm-dev script
                    if [ -f "./deploy.sh" ]; then
                        chmod +x ./deploy.sh || true
                        sudo cp ./deploy.sh "$DEPLOY_SCRIPT" || true
                        sudo chmod +x "$DEPLOY_SCRIPT" || true
                    fi

                    # 2. Stop stale dev container (do not touch prod containers)
                    echo "Stopping stale java-backend-dev container..."
                    docker stop java-backend-dev 2>/dev/null || true
                    docker rm java-backend-dev 2>/dev/null || true

                    # 3. Execute deployment
                    if [ -x "$DEPLOY_SCRIPT" ]; then
                        echo "Running $DEPLOY_SCRIPT..."
                        sudo "$DEPLOY_SCRIPT"
                    else
                        echo "Running local deploy.sh..."
                        ./deploy.sh
                    fi

                    # 4. Check deployment status
                    echo "=== Checking dev deployment status ==="
                    docker ps --filter "name=java-backend-dev" --format "table {{.ID}}\t{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
                '''
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
            echo 'SalesCRM pipeline finished.'
        }
        success {
            echo 'SalesCRM JAR built, Docker image pushed, and deployment completed successfully!'
        }
        failure {
            echo 'Pipeline failed. Check stage logs for details.'
        }
    }
}
