.PHONY: help build test run clean docker-build docker-up docker-down deploy

help:
	@echo "URL Shortener Service - Available Commands"
	@echo "==========================================="
	@echo "make build           - Build the project with Maven"
	@echo "make test            - Run all tests"
	@echo "make test-coverage   - Run tests with coverage report"
	@echo "make run             - Run application locally (requires Redis & Oracle)"
	@echo "make docker-build    - Build Docker image"
	@echo "make docker-up       - Start all services with Docker Compose"
	@echo "make docker-down     - Stop all Docker services"
	@echo "make docker-logs     - View Docker logs"
	@echo "make docker-clean    - Remove all containers and volumes"
	@echo "make clean           - Clean build artifacts"
	@echo "make package         - Package application as JAR"
	@echo "make verify          - Run Maven verify"
	@echo "make format          - Format code (if enabled)"
	@echo "make lint            - Run code quality checks"

# Building
build:
	@echo "Building project..."
	mvn clean compile

package:
	@echo "Packaging application..."
	mvn clean package -DskipTests

verify:
	@echo "Running verification..."
	mvn clean verify

# Testing
test:
	@echo "Running tests..."
	mvn test

test-coverage:
	@echo "Running tests with coverage..."
	mvn clean test jacoco:report
	@echo "Coverage report: target/site/jacoco/index.html"

# Running
run:
	@echo "Running application (requires Redis on 6379 and Oracle on 1521)..."
	mvn spring-boot:run

# Docker
docker-build:
	@echo "Building Docker image..."
	docker build -t url-shortener:latest .

docker-up:
	@echo "Starting Docker Compose services..."
	docker-compose up -d
	@echo "Services started:"
	@echo "  - Redis:   localhost:6379"
	@echo "  - Oracle:  localhost:1521"
	@echo "  - App:     http://localhost:8080"
	@docker-compose ps

docker-down:
	@echo "Stopping Docker Compose services..."
	docker-compose down

docker-logs:
	docker-compose logs -f

docker-clean:
	@echo "Removing all containers and volumes..."
	docker-compose down -v
	docker rmi url-shortener:latest

docker-ps:
	docker-compose ps

# Cleanup
clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	rm -rf target/
	rm -rf .m2/

# Code Quality
lint:
	@echo "Running code quality checks..."
	mvn checkstyle:check

format:
	@echo "Formatting code..."
	mvn fmt:format

sonar:
	@echo "Running SonarQube analysis..."
	mvn clean verify sonar:sonar

# Database
db-reset:
	@echo "Resetting database..."
	docker-compose exec -T oracle sqlplus system/oracle123@XE @init.sql

db-backup:
	@echo "Backing up database..."
	mkdir -p backups
	docker-compose exec -T oracle expdp system/oracle123@XE DIRECTORY=backup_dir DUMPFILE=backup_$(shell date +%Y%m%d_%H%M%S).dmp

# Deployment
deploy-staging:
	@echo "Deploying to staging..."
	docker tag url-shortener:latest url-shortener:staging
	# Add your staging deployment commands

deploy-prod:
	@echo "Deploying to production..."
	docker tag url-shortener:latest url-shortener:prod
	# Add your production deployment commands

# Health Checks
health:
	@echo "Checking service health..."
	@curl -s http://localhost:8080/api/v1/health && echo "" || echo "Service is down"

# Utility
version:
	@grep '<version>' pom.xml | head -1 | sed 's/.*<version>//;s/<\/version>.*//'

deps:
	@echo "Listing dependencies..."
	mvn dependency:tree

update-deps:
	@echo "Checking for dependency updates..."
	mvn versions:display-dependency-updates
