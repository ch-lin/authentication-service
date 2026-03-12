# Authentication Service (YouTube Data Hub)

![Java](https://img.shields.io/badge/Java-25%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![Security](https://img.shields.io/badge/Security-JWT%20%7C%20OAuth2-red)
![Architecture](https://img.shields.io/badge/Architecture-Microservice-blueviolet)
![Maven](https://img.shields.io/badge/Build-Maven-blue)
![Docker](https://img.shields.io/badge/Docker-Container-2496ED)
![License](https://img.shields.io/badge/License-MIT-green)

The **Authentication Service** is the dedicated security gatekeeper for the **YouTube Data Hub** ecosystem.

It operates as a standalone microservice responsible for identity management, token issuance, and request validation. It ensures that only authorized users and client applications (e.g., the YouTube-Hub UI, future mobile apps, or CLI tools) can access protected resources within the system.

## 🏗️ Architecture & Role

In the YouTube Data Hub microservices architecture, this service fulfills the following roles:

*   **Security Gatekeeper**: Centralizes all authentication logic, removing the need for other services (like the Downloader) to handle user credentials directly.
*   **Token Provider**: Issues **JSON Web Tokens (JWT)** signed with RSA keys.
*   **Stateless Validation**: Allows other services to verify identity via public key cryptography or by calling the validation endpoints.

### Design Philosophy
*   **Decoupled Security**: By isolating auth logic, we can update security protocols (e.g., rotating keys, adding OAuth2 providers) without redeploying the core application or downloader.
*   **Extensibility**: Designed to support future OAuth2 flows and machine-to-machine (M2M) communication.

## 🚀 Key Features

*   **JWT Management**: RSA-signed Access Token and Refresh Token issuance.
*   **Role-Based Access Control (RBAC)**: Manages permissions for Admin and User roles.
*   **Inter-Service Auth**: Validates Client IDs and Secrets for service-to-service communication (e.g., Hub talking to Downloader).
*   **Secure Storage**: Bcrypt hashing for password storage.

## 🧠 Authentication Strategy (Read Before Contributing)

This service implements a **simplified, stateless JWT architecture** designed specifically for a self-hosted, first-party ecosystem.

### 1. Direct Authentication (Resource Owner Password)
*   **RFC**: Based on **RFC 6749 Section 4.3** (Resource Owner Password Credentials Grant).
*   **Flow**: The UI (`YouTube-Hub-UI`) collects credentials and sends them directly to `/api/v1/auth/authenticate`.
*   **Rationale**: Since the UI is a trusted **First-Party Application**, we do not need the complexity of browser redirects (Authorization Code Flow).
*   **Implication**: There is **no `redirect_uri` validation** or storage required for user login. The response is a direct JSON payload containing the Token.

### 2. Machine-to-Machine (Client Credentials)
*   **RFC**: Compliant with **RFC 6749 Section 4.4** (Client Credentials Grant).
*   **Flow**: Backend services (like `Downloader`) authenticate using `client_id` and `client_secret` via `/api/v1/auth/client-authenticate`.
*   **Rationale**: Standard M2M communication without user intervention.

### 3. OAuth 2.0 Compliance
*   **Response Format**: While the *flow* is simplified, the **Response JSON** strictly follows RFC 6749 Section 5.1.
*   **Benefit**: This ensures compatibility with standard libraries (like NextAuth.js) without enforcing the redirect mechanism.

> **ℹ️ Note for Contributors**: The omission of Authorization Code Flow (Redirects) is a deliberate design choice for simplicity, not an oversight. We welcome PRs that introduce standard OAuth 2.0 flows (e.g., for Third-Party Login)! **However, if you do implement Redirect flows, please update this README to reflect the architectural change**, ensuring future contributors understand the system's evolution.

## 🛠️ Tech Stack

*   **Language**: Java (25+)
*   **Framework**: Spring Boot 3.5 (Spring Security)
*   **Database**: MySQL (User credentials & Roles)
*   **Cryptography**: RSA-2048 for JWT Signing
*   **Build Tool**: Maven

## 🔐 Security & Initialization

To prevent unauthorized access, this service follows a **"Bootstrap via Environment"** security model:

1.  **No Public Registration**: The `/api/auth/register` endpoint is **protected**. It requires an existing Admin JWT to access.
2.  **Initial Admin Creation**: On the very first startup, the service checks if the database is empty. If so, it reads the `INIT_ADMIN_*` environment variables to create the first Super Admin user.

This ensures that even within an internal network, an attacker cannot register a backdoor account without already having admin privileges.

### Recommended Production Settings
*   Ensure `INIT_ADMIN_PASSWORD` is strong.
*   After the first run, you can technically remove the `INIT_ADMIN` variables, though keeping them is harmless as the bootstrapper checks for existing users first.

## 📦 Prerequisites & Dependencies

Since this project was split from a mono-repo, it depends on the shared **Platform** library.

1.  **Platform Library**: You must build and install the `Platform` project locally before building this service.
    ```bash
    # Assuming you have the Platform repo cloned as a sibling
    cd ../Platform
    mvn clean install
    ```
2.  **Database**: A MySQL instance.
3.  **RSA Keys**: You need a pair of RSA keys (`private_key.pem` and `public_key.pem`) for signing tokens.

## ⚙️ Configuration

Create a `.env` file in the root directory. You can copy `.env.example` if available.

```properties
# Server Configuration
SERVER_PORT=8081

# Database
DB_URL=jdbc:mysql://localhost:3306/authentication
DB_USERNAME=root
DB_PASSWORD=secret

# Security (RSA Keys Paths or Content)
# Ensure these files exist and are readable
RSA_PRIVATE_KEY_PATH=/path/to/private_key.pem
RSA_PUBLIC_KEY_PATH=/path/to/public_key.pem

# JWT Settings
JWT_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=86400000
```

> **Note**: If you are using the `Setup-Scripts` repository from the main project, the `Init-secrets.sh` script can automatically generate the RSA keys and inject the configuration for you.

## 🏃‍♂️ Build & Run

### Local Development

```bash
# 1. Build the project (Multi-module structure)
cd authentication-backend
mvn clean package

# 2. Run the JAR
# The executable JAR is output to the 'bin' directory, not target
java -jar ../bin/authentication-service.jar
```

### Docker

> **Note**: Since this service depends on the shared `Platform` library, the Docker build context must include both repositories. Ensure `Platform` and `Authentication-Service` are siblings in your directory structure.

```bash
# 1. Navigate to the parent directory (containing both repos)
cd ..

# 2. Build the image
docker build -f Authentication-Service/authentication-backend/Dockerfile -t youtube-data-hub/auth-service .
```

```bash
# 3. Run container (Navigate back to service directory)
cd Authentication-Service
docker run -d \
  -p 8081:8081 \
  --env-file .env \
  -v $(pwd)/keys:/app/keys \
  youtube-data-hub/auth-service
```

## 🔌 API Endpoints (Overview)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/auth/authenticate` | Authenticate user and return JWTs. |
| `POST` | `/api/v1/auth/refresh` | Refresh access token using refresh token. |
| `POST` | `/api/v1/auth/validate` | Validate a token (used by other services). |
| `POST` | `/api/v1/auth/register` | Create a new user (**Requires Admin JWT**). |
| `POST` | `/api/v1/auth/client-authenticate` | Authenticate service/client (M2M). |
| `POST` | `/api/v1/auth/client-refresh` | Refresh client access token. |
| `POST` | `/api/v1/auth/client-register` | Register a new client (**Requires Admin JWT**). |
| `GET` | `/health` | Health check for Docker/K8s. |

## 🤝 Integration with Other Services

*   **YouTube-Hub-UI (UI)**: Directly sends user credentials to this service to obtain JWTs (Direct Auth pattern).
*   **Resource Servers (Hub Backend & Downloader)**: These services do not handle login logic. Instead, they **validate** incoming JWTs by verifying the RSA signature using this service's public key.

## 📜 License

MIT
