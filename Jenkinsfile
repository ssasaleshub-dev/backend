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
                    echo "=== Building SalesCRM Application JAR ==="
                    chmod +x ./mvnw || true

                    if command -v ./mvnw >/dev/null 2>&1 && ./mvnw -v >/dev/null 2>&1; then
                        echo "Using ./mvnw wrapper..."
                        ./mvnw -B clean package -DskipTests -Pprod
                    elif command -v mvn >/dev/null 2>&1; then
                        echo "Using system mvn..."
                        mvn -B clean package -DskipTests -Pprod
                    else
                        echo "Building JAR inside Maven Docker container..."
                        docker run --rm \
                            -v "$(pwd)":/app \
                            -v "$HOME/.m2":/root/.m2 \
                            -w /app \
                            maven:3.9.9-eclipse-temurin-21 \
                            mvn -B clean package -DskipTests -Pprod
                    fi

                    echo "Verifying generated JAR artifact:"
                    ls -lh target/*.jar
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    echo "=== Building Docker Image ==="
                    docker build \
                        -t "$IMAGE_NAME:${BUILD_NUMBER}" \
                        -t "$IMAGE_NAME:latest" \
                        -t "salescrm-platform-salescrm-app:latest" \
                        -t "salescrm-platform-salescrm-app" \
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
                        echo "$DOCKER_TOKEN" | docker login \
                            --username "$DOCKER_USER" \
                            --password-stdin

                        docker push "$IMAGE_NAME:${BUILD_NUMBER}"
                        docker push "$IMAGE_NAME:latest"
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    echo "=== Deploying Latest SalesCRM Backend ==="

                    # 1. Update /root/apps/backend if it exists
                    if [ -d "/root/apps/backend" ]; then
                        echo "Syncing /root/apps/backend with origin/main..."
                        sudo git -C /root/apps/backend fetch origin main 2>/dev/null || true
                        sudo git -C /root/apps/backend reset --hard origin/main 2>/dev/null || true
                    fi

                    # 2. Update /usr/local/bin/deploy-salescrm script
                    if [ -f "./deploy.sh" ]; then
                        chmod +x ./deploy.sh || true
                        sudo cp ./deploy.sh "$DEPLOY_SCRIPT" || true
                        sudo chmod +x "$DEPLOY_SCRIPT" || true
                    fi

                    # 3. Stop stale container
                    echo "Stopping stale salescrm-app containers..."
                    docker stop salescrm-app 2>/dev/null || true
                    docker rm salescrm-app 2>/dev/null || true
                    docker stop salescrm-platform-salescrm-app-1 2>/dev/null || true
                    docker rm salescrm-platform-salescrm-app-1 2>/dev/null || true

                    # 4. Execute deployment
                    if [ -x "$DEPLOY_SCRIPT" ]; then
                        echo "Running $DEPLOY_SCRIPT..."
                        sudo "$DEPLOY_SCRIPT"
                    else
                        echo "Running local deploy.sh..."
                        ./deploy.sh
                    fi

                    # 5. Check deployment status
                    echo "=== Checking deployment status ==="
                    docker ps --filter "name=salescrm-app" --format "table {{.ID}}\t{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
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
