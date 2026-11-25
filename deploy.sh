#!/usr/bin/env bash
set -e

TARGET_ENV="${1:-${TARGET_ENV:-prod}}"

echo "=========================================="
echo " Starting SalesCRM (${TARGET_ENV}) Deployment "
echo "=========================================="

if [ "$TARGET_ENV" = "dev" ]; then
    APP_DIR="/root/apps/dev"
    COMPOSE_FILE="compose.yml"
    NETWORK_NAME="dev-backend-network"
    POSTGRES_SERVICE="postgres"
    APP_SERVICE="backend"
    CONTAINER_NAME="java-backend-dev"
    HOST_PORT="8081"
    SPRING_PROFILE="dev"
    export IMAGE_NAME="${2:-${IMAGE_NAME:-ssasaleshub/backend:dev}}"
else
    APP_DIR="/root/apps/backend"
    COMPOSE_FILE="docker-compose.yml"
    NETWORK_NAME="salescrm-net"
    POSTGRES_SERVICE="salescrm-postgres"
    APP_SERVICE="salescrm-app"
    CONTAINER_NAME="salescrm-app"
    HOST_PORT="8080"
    SPRING_PROFILE="prod"
    export IMAGE_NAME="${2:-${IMAGE_NAME:-ssasaleshub/backend:latest}}"
fi

# 1. Ensure docker network exists
echo "Ensuring docker network ($NETWORK_NAME) exists..."
docker network create "$NETWORK_NAME" 2>/dev/null || true

# 2. Check latest image (prioritize fresh locally-built image)
if ! docker image inspect "$IMAGE_NAME" >/dev/null 2>&1; then
    echo "Pulling latest image: $IMAGE_NAME..."
    docker pull "$IMAGE_NAME" || true
else
    echo "Using fresh local image: $IMAGE_NAME"
fi

# 3. Deploy via Docker Compose
if [ -d "$APP_DIR" ] && [ -f "$APP_DIR/$COMPOSE_FILE" ]; then
    echo "Deploying via Docker Compose in $APP_DIR ($COMPOSE_FILE)..."
    cd "$APP_DIR"

    docker compose -f "$COMPOSE_FILE" up -d "$POSTGRES_SERVICE"

    echo "Recreating $APP_SERVICE container with latest image ($IMAGE_NAME)..."
    docker compose -f "$COMPOSE_FILE" up -d --force-recreate "$APP_SERVICE"
elif [ "$TARGET_ENV" = "prod" ] && [ -f "./docker-compose.yml" ]; then
    echo "Deploying via Docker Compose in current directory $(pwd)..."
    docker compose up -d "$POSTGRES_SERVICE"
    docker compose up -d --force-recreate "$APP_SERVICE"
else
    echo "Compose file not found. Deploying container ($CONTAINER_NAME) directly..."
    docker stop "$CONTAINER_NAME" 2>/dev/null || true
    docker rm "$CONTAINER_NAME" 2>/dev/null || true
    docker run -d \
        --name "$CONTAINER_NAME" \
        --restart unless-stopped \
        --network "$NETWORK_NAME" \
        -p "${HOST_PORT}:8080" \
        -e "SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}" \
        -e _JAVA_OPTIONS="-Xms256m -Xmx512m" \
        "$IMAGE_NAME"
fi

# 4. Clean up unused/dangling docker images
echo "Cleaning up dangling images..."
docker image prune -f

echo "=========================================="
echo " SalesCRM (${TARGET_ENV}) Deployed Successfully! "
echo "=========================================="
