.PHONY: help setup login build push up down logs clean

REGISTRY_URL ?= ghcr.io/sadlowe
BACKEND_IMAGE  := steiner-tree-solver-backend:1.0.0
FRONTEND_IMAGE := steiner-tree-solver-frontend:1.0.0
BACKEND_REMOTE  := $(REGISTRY_URL)/steiner-tree-solver-backend:1.0.0
FRONTEND_REMOTE := $(REGISTRY_URL)/steiner-tree-solver-frontend:1.0.0
COMPOSE_FILE := ./deploy/docker-compose.yaml

help:
	@echo "Makefile for Steiner Tree Solver"
	@echo ""
	@echo "Usage:"
	@echo "  make help        Show this help message"
	@echo "  make setup       Create deploy/.env from .env.example (first-time setup)"
	@echo "  make login       Log in to the Docker registry (ghcr.io)"
	@echo "  make build       Build Docker images (tagged for local use and registry)"
	@echo "  make push        Push Docker images to the registry"
	@echo "  make up          Start the application stack using Docker Compose"
	@echo "  make down        Stop the application stack"
	@echo "  make logs        Show logs for the application stack"
	@echo "  make clean       Remove Docker images and stop the stack"
	@echo ""

# Crée deploy/.env depuis l'exemple si le fichier n'existe pas encore
setup:
	@if [ ! -f deploy/.env ]; then \
		cp deploy/.env.example deploy/.env; \
		echo "deploy/.env created — edit it before running make up."; \
	else \
		echo "deploy/.env already exists, skipping."; \
	fi

login:
	docker login ghcr.io

# Double tag : local (pour docker compose) + remote (pour le push)
build:
	@echo "Building backend Docker image..."
	docker build -t $(BACKEND_IMAGE) -t $(BACKEND_REMOTE) ./backend
	@echo "Building frontend Docker image..."
	docker build -t $(FRONTEND_IMAGE) -t $(FRONTEND_REMOTE) ./frontend

push:
	@echo "Pushing backend Docker image..."
	docker push $(BACKEND_REMOTE)
	@echo "Pushing frontend Docker image..."
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
