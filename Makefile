.PHONY: help build push up down logs clean

REGISTRY_URL ?= ghcr.io/sadlowe
BACKEND_IMAGE := steiner-tree-solver-backend:1.0.0
FRONTEND_IMAGE := steiner-tree-solver-frontend:1.0.0
BACKEND_REMOTE := $(REGISTRY_URL)/steiner-tree-solver-backend:1.0.0
FRONTEND_REMOTE := $(REGISTRY_URL)/steiner-tree-solver-frontend:1.0.0
COMPOSE_FILE := ./deploy/docker-compose.yaml

help:
	@echo "Makefile for Steiner Tree Solver"
	@echo ""
	@echo "Usage:"
	@echo "  make help        Show this help message"
	@echo "  make build       Build Docker images for backend and frontend"
	@echo "  make push        Push Docker images to the registry"
	@echo "  make up          Start the application stack using Docker Compose"
	@echo "  make down        Stop the application stack"
	@echo "  make logs        Show logs for the application stack"
	@echo "  make clean       Remove Docker images and stop the stack"
	@echo ""

build:
	@echo "Building backend Docker image..."
	docker build -t $(BACKEND_IMAGE) ./backend
	@echo "Building frontend Docker image..."
	docker build -t $(FRONTEND_IMAGE) ./frontend

push:
	@echo "Pushing backend Docker image..."
	docker tag $(BACKEND_IMAGE) $(BACKEND_REMOTE)
	docker push $(BACKEND_REMOTE)
	@echo "Pushing frontend Docker image..."
	docker tag $(FRONTEND_IMAGE) $(FRONTEND_REMOTE)
	docker push $(FRONTEND_REMOTE)

up:
	@echo "Starting the application stack..."
	docker compose -f $(COMPOSE_FILE) up -d

down:
	@echo "Stopping the application stack..."
	-docker compose -f $(COMPOSE_FILE) down $(RMI)

logs:
	@echo "Showing application logs..."
	docker compose -f $(COMPOSE_FILE) logs -f

clean:
	@echo "Stopping stack and removing images..."
	$(MAKE) down RMI="--rmi all"
	@echo "Cleanup complete."

