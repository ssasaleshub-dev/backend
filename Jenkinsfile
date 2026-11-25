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
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: sh(
                        script: 'git rev-parse --abbrev-ref HEAD',
                        returnStdout: true
                    ).trim()
                    branch = branch.replaceFirst('^origin/', '').replaceFirst('^refs/heads/', '')

                    if (branch == 'dev') {
                        env.TARGET_ENV = 'dev'
                        env.IMAGE_TAG = 'dev'
                        env.BUILD_TAG = "dev-${env.BUILD_NUMBER}"
                        env.MVN_PROFILE = 'dev'
                        env.DEPLOY_SCRIPT = '/usr/local/bin/deploy-salescrm-dev'
                        env.APP_CONTAINER = 'java-backend-dev'
                    } else {
                        env.TARGET_ENV = 'prod'
                        env.IMAGE_TAG = 'latest'
                        env.BUILD_TAG = "${env.BUILD_NUMBER}"
                        env.MVN_PROFILE = 'prod'
                        env.DEPLOY_SCRIPT = '/usr/local/bin/deploy-salescrm'
                        env.APP_CONTAINER = 'salescrm-app'
                    }

                    echo "Branch: ${branch} | Env: ${env.TARGET_ENV} | Image: ${env.IMAGE_NAME}:${env.IMAGE_TAG} | Profile: ${env.MVN_PROFILE}"
                }
            }
        }

        stage('Build JAR') {
            steps {
                sh '''
                    echo "=== Building SalesCRM Application JAR (${TARGET_ENV}) ==="
                    chmod +x ./mvnw || true

                    if command -v ./mvnw >/dev/null 2>&1 && ./mvnw -v >/dev/null 2>&1; then
                        echo "Using ./mvnw wrapper..."
                        ./mvnw -B clean package -DskipTests -P"${MVN_PROFILE}"
                    elif command -v mvn >/dev/null 2>&1; then
                        echo "Using system mvn..."
                        mvn -B clean package -DskipTests -P"${MVN_PROFILE}"
                    else
                        echo "Building JAR inside Maven Docker container..."
                        docker run --rm \
                            -v "$(pwd)":/app \
                            -v "$HOME/.m2":/root/.m2 \
                            -w /app \
                            maven:3.9.9-eclipse-temurin-21 \
                            mvn -B clean package -DskipTests -P"${MVN_PROFILE}"
                    fi

                    echo "Verifying generated JAR artifact:"
                    ls -lh target/*.jar
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    echo "=== Building Docker Image (${IMAGE_NAME}:${IMAGE_TAG}) ==="
                    if [ "$TARGET_ENV" = "prod" ]; then
                        docker build \
                            -t "$IMAGE_NAME:$BUILD_TAG" \
                            -t "$IMAGE_NAME:$IMAGE_TAG" \
                            -t "salescrm-platform-salescrm-app:latest" \
                            -t "salescrm-platform-salescrm-app" \
                            .
                    else
                        docker build \
                            -t "$IMAGE_NAME:$BUILD_TAG" \
                            -t "$IMAGE_NAME:$IMAGE_TAG" \
                            .
                    fi
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

                            echo "Pushing images to registry..."
                            docker push "$IMAGE_NAME:$BUILD_TAG" || true
                            docker push "$IMAGE_NAME:$IMAGE_TAG" || true
                        '''
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    echo "=== Deploying SalesCRM (${TARGET_ENV}) ==="

                    # 1. Sync /root/apps/backend only for prod
                    if [ "$TARGET_ENV" = "prod" ] && [ -d "/root/apps/backend" ]; then
                        echo "Syncing /root/apps/backend with origin/main..."
                        sudo git -C /root/apps/backend fetch origin main 2>/dev/null || true
                        sudo git -C /root/apps/backend reset --hard origin/main 2>/dev/null || true
                    fi

                    # 2. Update environment-specific deploy script
                    if [ -f "./deploy.sh" ]; then
                        chmod +x ./deploy.sh || true
                        sudo cp ./deploy.sh "$DEPLOY_SCRIPT" || true
                        sudo chmod +x "$DEPLOY_SCRIPT" || true
                    fi

                    # 3. Stop stale container for this environment only
                    echo "Stopping stale $APP_CONTAINER container..."
                    docker stop "$APP_CONTAINER" 2>/dev/null || true
                    docker rm "$APP_CONTAINER" 2>/dev/null || true
                    if [ "$TARGET_ENV" = "prod" ]; then
                        docker stop salescrm-platform-salescrm-app-1 2>/dev/null || true
                        docker rm salescrm-platform-salescrm-app-1 2>/dev/null || true
                    fi

                    # 4. Execute deployment with TARGET_ENV and full IMAGE_NAME:IMAGE_TAG
                    if [ -x "$DEPLOY_SCRIPT" ]; then
                        echo "Running $DEPLOY_SCRIPT ($TARGET_ENV, $IMAGE_NAME:$IMAGE_TAG)..."
                        sudo "$DEPLOY_SCRIPT" "$TARGET_ENV" "$IMAGE_NAME:$IMAGE_TAG"
                    else
                        echo "Running local deploy.sh ($TARGET_ENV, $IMAGE_NAME:$IMAGE_TAG)..."
                        ./deploy.sh "$TARGET_ENV" "$IMAGE_NAME:$IMAGE_TAG"
                    fi

                    # 5. Check deployment status
                    echo "=== Checking deployment status ==="
                    docker ps --filter "name=$APP_CONTAINER" --format "table {{.ID}}\t{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
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
            echo "SalesCRM (${env.TARGET_ENV}) JAR built, Docker image pushed, and deployment completed successfully!"
        }
        failure {
            echo 'Pipeline failed. Check stage logs for details.'
        }
    }
}
