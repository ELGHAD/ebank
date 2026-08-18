# 🏦 eBank — Modern Banking Application

[![Build](https://img.shields.io/badge/build-passing-brightgreen?style=for-the-badge&logo=github)](https://github.com/)
[![Version](https://img.shields.io/badge/version-1.0.0-blue?style=for-the-badge)](https://github.com/)
[![License](https://img.shields.io/badge/license-None-lightgrey?style=for-the-badge)](LICENSE)
[![Tech Stack](https://img.shields.io/badge/stack-Java%20%7C%20React%20%7C%20Docker-blueviolet?style=for-the-badge)](https://github.com/)
[![Status](https://img.shields.io/badge/status-in__progress-yellow?style=for-the-badge)](https://github.com/)

<p align="center">
  <strong>eBank</strong> is a full-stack digital banking application designed for seamless performance, role-based security, and cloud readiness. Built as a monorepo, it features JWT authentication, administrative account/customer management, and an intuitive client dashboard.
</p>

---

## 📌 Overview & Problem Solved

`eBank` bridges the gap between educational demo projects and enterprise-ready architecture by offering a clean, lightweight, monorepo-based banking solution consisting of:

- 🔒 **Secure Backend**: Java Spring Boot microservice architecture.
- 🎨 **Modern Frontend**: Interactive React SPA.
- 🗄️ **Database**: Robust MySQL persistence layer.
- 🐳 **Containerization**: One-command local setup via Docker Compose & Nginx proxying.

### 🎯 Key Architectural Goals

Many demo banking platforms suffer from bloated configurations or tangled codebases. **eBank** resolves this by delivering:

*   **Role-Based Access Control (RBAC)**: Enforced separation between `CLIENT` and `AGENT_GUICHET` (Bank Agent) roles.
*   **Production-Ready Patterns**: Real-world REST API design, JWT stateless authentication, and Nginx reverse proxying.
*   **Developer Experience**: Instant environment configuration and local setup via containerization.

---

## ✨ Key Features

### 🌐 Global Capabilities
- **Stateless Authentication**: End-to-end security via JSON Web Tokens (JWT).
- **Spring Security**: Method-level and path-based request authorization.
- **Clean Architecture**: Backend organized into `DTOs`, `Entities`, `Repositories`, `Services`, and `Controllers`.
- **Responsive UI**: Sleek, mobile-friendly interface built with React.
- **Docker Integration**: Multi-container setup for effortless local orchestration.

### 👨‍💼 Admin Module (`AGENT_GUICHET`)
- **Customer Onboarding**: Create and manage customer profiles.
- **Account Management**: Provision new bank accounts linked to specific customers.
- **Customer Lookup**: Fast querying of customer accounts via `clientId`.

### 👤 Client Dashboard (`CLIENT`)
- **Secure Portal**: Protected client authentication flow.
- **Account Overview**: Real-time balance and profile insights.
- **Fund Transfer UI**: Intuitive web interface for mock money transfers.

### ⚙️ Infrastructure & DevOps
- **MySQL DB**: Relational data management for persistence.
- **Containerized Environment**: Pre-configured Dockerfiles for instant startup.
- **Nginx Reverse Proxy**: Efficient `/api` routing and dynamic asset delivery.

---

## 🛠️ Tech Stack

| Layer | Technology | Version / Details |
|---|---|---|
| **Backend** | ![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white) | Java 17 LTS |
| **Framework** | ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?logo=springboot&logoColor=white) | Spring Boot 3.2.5, Spring Security, Spring Data JPA |
| **Build Tool** | ![Maven](https://img.shields.io/badge/Maven-3.8.0-C71A36?logo=apachemaven&logoColor=white) | Dependency & Lifecycle Management |
| **Database** | ![MySQL](https://img.shields.io/badge/MySQL-8.1-4479A1?logo=mysql&logoColor=white) | Relational Database |
| **Frontend** | ![React](https://img.shields.io/badge/React-19.2.1-61DAFB?logo=react&logoColor=black) | Single Page Application Framework |
| **Routing** | ![React Router](https://img.shields.io/badge/React_Router-7.10.1-CA4245?logo=reactrouter&logoColor=white) | Client-side Navigation |
| **HTTP Client**| ![Axios](https://img.shields.io/badge/Axios-1.13.2-5A29E4?logo=axios&logoColor=white) | REST API Requests |
| **DevOps** | ![Docker](https://img.shields.io/badge/Docker-24.0.5-2496ED?logo=docker&logoColor=white) ![Docker Compose](https://img.shields.io/badge/Docker_Compose-1.29.2-2496ED?logo=docker&logoColor=white) | Multi-container Environment & Orchestration |

---

## 🏗️ Project Architecture & Structure

text
        ebank/
        ├── .env.example
        ├── docker-compose.yml
        ├── .gitignore
        ├── ebank-backend/
        │   ├── Dockerfile
        │   ├── pom.xml
        │   ├── .mvn/
        │   ├── src/
        │   │   ├── main/
        │   │   │   ├── java/com/example/ebank/
        │   │   │   │   ├── config/         # Spring Security, JWT filters & configurations
        │   │   │   │   ├── dto/            # REST request/response payloads
        │   │   │   │   ├── entities/       # JPA Entities (Client, BankAccount, AppUser, etc.)
        │   │   │   │   ├── repositories/   # Spring Data JPA repositories
        │   │   │   │   ├── services/       # Core business logic
        │   │   │   │   └── web/            # REST Controllers
        │   │   └── resources/
        │   │       └── application.properties.example
        │   └── target/
        └── ebank-frontend/
            ├── Dockerfile
            ├── nginx.conf
            ├── package.json
            ├── public/
            └── src/
                ├── api/                    # Axios client instance
                ├── components/             # Reusable UI components
                ├── context/                # AuthContext state management
                ├── pages/                  # Main views/routes
                └── services/               # HTTP service abstractions

🚀 Quick Start Guide
      Prerequisites
      Ensure you have the following installed locally:
      
      Docker & Docker Compose
      
      Java 17+ (if running backend locally)
      
      Node.js 20+ (if running frontend locally)
      
      Maven (optional, Maven wrapper ./mvnw is included)
      
      1. Environment Setup
      Copy the sample environment file to create your local .env:
      
      Bash
      # Linux / macOS
      cp .env.example .env
      
      # Windows PowerShell
      copy .env.example .env
      Review and customize .env values:
      
      Code snippet
      MYSQL_ROOT_PASSWORD=ChangeMeRootPassword
      MYSQL_DATABASE=ebank_db
      MYSQL_USER=ebank
      MYSQL_PASSWORD=ChangeMeDbPassword
      APP_JWT_SECRET=ChangeMeJwtSecret
      APP_JWT_EXPIRATION=3600000
      BACKEND_PORT=8080
      FRONTEND_PORT=3000
      REACT_APP_API_BASE_URL=http://localhost:8080
      2. Run with Docker Compose (Recommended)
      Start the entire application stack (MySQL, Backend, Frontend, and Nginx) with a single command:
      
      Bash
      docker compose up --build -d
      Access the application at http://localhost:3000.
      
      3. Alternative: Running Locally without Docker
      Backend Setup
      Bash
      cd ebank-backend
      ./mvnw clean package
      ./mvnw spring-boot:run
      Frontend Setup
      Bash
      cd ebank-frontend
      npm install
      npm start
      Note: Ensure REACT_APP_API_BASE_URL points to your active backend (default: http://localhost:8080).

📊 API Reference
      🔑 Authentication Endpoints
      POST /api/auth/login
      Authenticates user credentials and returns a JWT token.
      
      Request Body:
      
      JSON
      {
        "username": "user@example.com",
        "password": "password"
      }
      Response (200 OK):
      
      JSON
      {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "username": "admin",
        "role": "AGENT_GUICHET",
        "expiresIn": 3600000
      }
      GET /api/auth/me
      Validates active JWT session and retrieves current user profile.
      
      👤 Client Endpoints
      GET /api/client/accounts
      Retrieves all bank accounts belonging to the authenticated client.
      
      👨‍💼 Admin Endpoints (AGENT_GUICHET)
      POST /api/admin/clients
      Registers a new client.
      
      JSON
      {
        "identityRef": "ID-12345",
        "firstname": "John",
        "lastname": "Doe",
        "birthDate": "1990-05-12",
        "email": "john.doe@example.com",
        "address": "10 Baker Street, London"
      }
      POST /api/admin/accounts
      Creates a bank account assigned to a customer.
      
      JSON
      {
        "clientId": 1,
        "rib": "GB82WEST12345698765432",
        "balance": 1200.50
      }
      GET /api/admin/clients/{clientId}/accounts
      Fetches all account details associated with a given clientId.

🧪 Testing & Quality Assurance
    Run automated unit and integration tests across services:
    
    Backend Testing
    Bash
    cd ebank-backend
    ./mvnw test
    Frontend Testing
    Bash
    cd ebank-frontend
    npm test
    🗺️ Roadmap & Future Enhancements
    [ ] Complete transaction processing engine on backend (/api/client/transfers).
    
    [ ] Add an Admin Analytics Dashboard with interactive visual statistics.
    
    [ ] Implement pagination & transaction history filtering.
    
    [ ] Increase test coverage (Integration & E2E tests).
    
    [ ] Cloud Deployment Guides (AWS ECS / Azure App Service / Kubernetes).

🤝 Contributing
  Contributions are warmly welcomed! To contribute:
  
  Fork the repository.
  
  Create your feature branch: git checkout -b feature/AmazingFeature.
  
  Commit your changes: git commit -m 'Add some AmazingFeature'.
  
  Push to the branch: git push origin feature/AmazingFeature.
  
  Open a Pull Request.

📄 License
  This project is currently distributed without an explicit license. If you intend to use or adapt this repository publicly, please consider adding an appropriate open-source license (such as MIT)
