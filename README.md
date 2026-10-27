# ProFinder Backend

[![CI](https://github.com/Strogolsky/proFinder_backend/actions/workflows/ci.yml/badge.svg)](https://github.com/Strogolsky/proFinder_backend/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Strogolsky/proFinder_backend/graph/badge.svg)](https://codecov.io/gh/Strogolsky/proFinder_backend)
[![SonarCloud Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=Strogolsky_proFinder_backend&metric=alert_status)](https://sonarcloud.io/dashboard?id=Strogolsky_proFinder_backend)

> **Note:** This repository contains only the **backend component** of the ProFinder platform. The client applications are developed separately.

## 📖 Table of Contents

- [Service Overview](#-service-overview)
- [Documentation](#-documentation)
- [Technology Stack](#-technology-stack)
- [Architecture & Features](#-architecture--features)
- [First Run](#-first-run)

## 🚀 Service Overview

**ProFinder** is a comprehensive service marketplace platform designed to bridge the gap between customers seeking professional services and skilled specialists. 

The platform enables customers to post job listings for various tasks and connect with qualified professionals. Specialists can showcase their expertise, browse available opportunities, and apply for projects that match their skills.

This backend service powers the core functionality including user management, job postings, search capabilities, real-time communication, and transaction processing.

## 📚 Documentation

Full design and architecture documentation is in [`docs/`](docs/) (start at
[`docs/Home.md`](docs/Home.md)), synced both ways with the
[GitHub Wiki](https://github.com/Strogolsky/proFinder_backend/wiki).

Those documents describe the **target architecture** — a set of microservices on a
RabbitMQ event bus. This repository is currently a single Quarkus module that
implements a subset of it; where the two disagree, the docs are the intended
design. See [`.github/wiki-sync.md`](.github/wiki-sync.md) for how the sync works.

## 🛠 Technology Stack

### Core Technologies
- Java
- Quarkus
- Maven

### Database & Storage
- PostgreSQL
- Hibernate ORM with Panache
- Elasticsearch
- Redis
- MinIO

### Security & Authentication
- JWT (SmallRye JWT)
- Quarkus Security
- BCrypt
- Hibernate Validator

### Additional Components
- WebSockets
- MapStruct
- Lombok
- Mailer
- Micrometer + Prometheus
- OpenAPI

## 🏗 Architecture & Features

### Platform Features
- **User Management** - Separate profiles for Customers and Professionals
- **Job Listings System** - Customers create service requests
-  **Specialist Search** - Ability to find professionals through search functionality
- **Application System** - Professionals apply to relevant projects
- **Review & Rating System** - Feedback and reputation management
- **Real-time Chat** - WebSocket-based messaging between users
- **Secure Authentication** - JWT-based security with role management

### Operational Features
- **Containerization** - Docker support for easy deployment
- **Performance Monitoring** - Integrated metrics with Prometheus
- **API Documentation** - Auto-generated OpenAPI specifications
- **Data Validation** - Multi-layer input validation

## 🚦 First Run

Secrets are not stored in the repository. Before starting the project:

1. Copy the example environment file and fill in the values (it is ignored by git and is read by both Quarkus and `docker compose`):

   ```bash
   cp .env.example .env
   ```

2. Generate a throw-away JWT key pair for local development (written to `keys/`, ignored by git):

   ```bash
   ./scripts/generate-dev-keys.sh
   ```

3. Start the infrastructure and the application:

   ```bash
   docker-compose up -d
   ./mvnw quarkus:dev
   ```

In production the same variables (`DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH`) are provided by the environment, and `docker-compose.prod.yml` mounts `./keys` into the backend container.
