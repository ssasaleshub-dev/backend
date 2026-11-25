#!/usr/bin/env bash
set -e

echo "=========================================="
echo " Starting SalesCRM Dev Backend Deployment "
echo "=========================================="

APP_DIR="/root/apps/dev"
export IMAGE_NAME="ssasaleshub/backend:dev"

# 1. Ensure dev docker network exists
echo "Ensuring dev docker network exists..."
docker network create dev-backend-network 2>/dev/null || true

# 2. Check latest dev image (prioritize fresh locally-built image)
if ! docker image inspect "$IMAGE_NAME" >/dev/null 2>&1; then
    echo "Pulling latest dev image: $IMAGE_NAME..."
    docker pull "$IMAGE_NAME" || true
else
    echo "Using fresh local dev image: $IMAGE_NAME"
fi

# 3. Deploy via existing /root/apps/dev/compose.yml
if [ -d "$APP_DIR" ] && [ -f "$APP_DIR/compose.yml" ]; then
    echo "Deploying via Docker Compose in $APP_DIR..."
    cd "$APP_DIR"

    # Ensure dev postgres is running
    docker compose -f compose.yml up -d postgres

    # Force recreate backend container with the latest dev image
    echo "Recreating java-backend-dev container with latest image ($IMAGE_NAME)..."
    docker compose -f compose.yml up -d --force-recreate backend
else
    echo "Compose file not found in $APP_DIR. Deploying dev container directly..."
    docker stop java-backend-dev 2>/dev/null || true
    docker rm java-backend-dev 2>/dev/null || true
    docker run -d \
        --name java-backend-dev \
        --restart unless-stopped \
        --network dev-backend-network \
        -p 8081:8080 \
        -e SPRING_PROFILES_ACTIVE=dev \
        -e SPRING_R2DBC_URL=r2dbc:postgresql://postgres:5432/salescrm_dev \
        -e SPRING_R2DBC_USERNAME="${DB_USER:-salesCRM}" \
        -e SPRING_R2DBC_PASSWORD="${DB_PASSWORD:-salesCRM}" \
        -e SPRING_LIQUIBASE_URL=jdbc:postgresql://postgres:5432/salescrm_dev \
        -e SPRING_LIQUIBASE_USER=salesCRM \
        -e SPRING_LIQUIBASE_PASSWORD=salesCRM \
        -e _JAVA_OPTIONS="-Xms256m -Xmx512m" \
        "$IMAGE_NAME"
fi

# 4. Clean up unused/dangling docker images
echo "Cleaning up dangling images..."
docker image prune -f

echo "=========================================="
echo " SalesCRM Dev Backend Deployed Successfully! "
echo "=========================================="
