#!/usr/bin/env bash
set -e

echo "=========================================="
echo " Starting SalesCRM Backend Deployment "
echo "=========================================="

APP_DIR="/root/apps/backend"
IMAGE_NAME="ssasaleshub/backend:latest"

# 1. Ensure docker network exists
echo "Ensuring docker network exists..."
docker network create salescrm-net 2>/dev/null || true

# 2. Pull the latest image
echo "Pulling latest image: $IMAGE_NAME..."
docker pull "$IMAGE_NAME" || true

# 3. Deploy via Docker Compose
if [ -d "$APP_DIR" ] && [ -f "$APP_DIR/docker-compose.yml" ]; then
    echo "Deploying via Docker Compose in $APP_DIR..."
    cd "$APP_DIR"

    # Ensure postgres is running
    docker compose up -d salescrm-postgres

    # Force recreate salescrm-app container with the latest image
    echo "Recreating salescrm-app container with latest image..."
    docker compose up -d --no-build --force-recreate salescrm-app
elif [ -f "./docker-compose.yml" ]; then
    echo "Deploying via Docker Compose in current directory $(pwd)..."
    docker compose up -d salescrm-postgres
    docker compose up -d --no-build --force-recreate salescrm-app
else
    echo "Docker compose file not found. Deploying container directly..."
    docker stop salescrm-app 2>/dev/null || true
    docker rm salescrm-app 2>/dev/null || true
    docker run -d \
        --name salescrm-app \
        --restart unless-stopped \
        --network salescrm-net \
        -p 8080:8080 \
        -e SPRING_PROFILES_ACTIVE=prod \
        -e SPRING_R2DBC_URL=r2dbc:postgresql://salescrm-postgres:5432/salesCRM \
        -e SPRING_R2DBC_USERNAME=salesCRM \
        -e SPRING_R2DBC_PASSWORD=salescrm_secret \
        -e SPRING_LIQUIBASE_URL=jdbc:postgresql://salescrm-postgres:5432/salesCRM \
        -e SPRING_LIQUIBASE_USER=salesCRM \
        -e SPRING_LIQUIBASE_PASSWORD=salescrm_secret \
        -e JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64_SECRET=fd54a454659368e69d6e53ea04554e296d6f1a54c48bdd39da918a39795da4e0cac91738874fa816d9b7bee52cca7793be663c705081c25c33d24f01141c52d1 \
        -e _JAVA_OPTIONS="-Xms256m -Xmx512m" \
        "$IMAGE_NAME"
fi

# 4. Clean up unused/dangling docker images
echo "Cleaning up dangling images..."
docker image prune -f

echo "=========================================="
echo " SalesCRM Backend Deployed Successfully! "
echo "=========================================="
