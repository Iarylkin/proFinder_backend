# ProFinder Backend

[![CI](https://github.com/Strogolsky/proFinder_backend/actions/workflows/ci.yml/badge.svg)](https://github.com/Strogolsky/proFinder_backend/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Strogolsky/proFinder_backend/graph/badge.svg)](https://codecov.io/gh/Strogolsky/proFinder_backend)

> **Note:** This repository contains only the **backend component** of the ProFinder platform. The client applications are developed separately.

## 📖 Table of Contents

- [Service Overview](#-service-overview)
- [Documentation](#-documentation)
- [Technology Stack](#-technology-stack)
- [Architecture & Features](#-architecture--features)

## 🚀 Service Overview

**ProFinder** is a comprehensive service marketplace platform designed to bridge the gap between clients seeking professional services and skilled specialists. 

The platform enables clients to post job listings for various tasks and connect with qualified professionals. Specialists can showcase their expertise, browse available opportunities, and apply for projects that match their skills.

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
- **User Management** - Separate profiles for Clients and Professionals
- **Job Listings System** - Clients create service requests
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
