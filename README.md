# PlacementPro Backend

A comprehensive Spring Boot backend for managing the complete placement ecosystem. It provides secure APIs for job management, application tracking, user authentication, and real-time notifications with role-based access control.

---

## 🎯 Features

- **JWT Authentication**: Secure token-based authentication with HttpOnly cookie storage
- **Role-Based Access Control**: Granular permissions for Students, Employers, Placement Officers, and Administrators
- **Comprehensive Job Management**: Create, update, delete, and search job postings
- **Application Tracking**: Complete lifecycle management for job applications
- **Resume Upload**: Secure file upload and storage for resumes
- **Real-Time Notifications**: Event-driven notification system with read status tracking
- **Analytics Dashboard**: Placement statistics and insights for different user roles
- **Email Notifications**: SMTP integration for email communications
- **Rate Limiting**: API protection against abuse with configurable limits
- **Caching**: Performance optimization with Caffeine cache
- **Exception Handling**: Comprehensive error handling with meaningful responses
- **Logging**: Structured logging for debugging and monitoring
- **CORS Support**: Secure cross-origin resource sharing
- **Password Security**: BCrypt password hashing and reset functionality

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Programming Language |
| Spring Boot | 3.2.4 | Framework |
| Spring Security | 3.2.4 | Authentication & Authorization |
| Spring Data JPA | 3.2.4 | Data Access |
| Hibernate | 6.4.4 | ORM |
| JWT (JJWT) | 0.11.5 | Token Generation & Validation |
| MySQL | 8.0+ | Database |
| Maven | 3.9+ | Build Tool |
| Bucket4j | 7.6.0 | Rate Limiting |
| Caffeine | 3.1.8 | Caching |
| ModelMapper | 3.2.0 | DTO Mapping |
| Lombok | 1.18.30 | Code Generation |

---

## 🏗️ Architecture

The application follows a layered architecture pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                     REST API Layer                           │
│  (Controllers - HTTP endpoints and request handling)         │
├─────────────────────────────────────────────────────────────┤
│                   Service Layer                              │
│  (Business Logic - Application rules and workflows)          │
├─────────────────────────────────────────────────────────────┤
│                Repository Layer                              │
│  (Data Access - Database interactions via JPA)               │
├─────────────────────────────────────────────────────────────┤
│                  Entity Layer                                │
│  (Domain Models - Database entities)                         │
├─────────────────────────────────────────────────────────────┤
│              Security & Configuration Layer                  │
│  (JWT, CORS, Exception Handling, Caching)                   │
└─────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

- **Controller Layer**: Handles HTTP requests, validates input, delegates to services
- **Service Layer**: Contains business logic, transaction management, validation
- **Repository Layer**: Database operations using Spring Data JPA
- **Entity Layer**: JPA entities with relationship mappings
- **Security Layer**: JWT validation, role-based authorization, encryption

---

## 🗄️ Database Schema

### Core Tables

#### `users`
Stores user account information for all user types.

```
users
├── id (PK)
├── name
├── email (UNIQUE)
├── password (hashed with BCrypt)
├── role (ENUM: STUDENT, EMPLOYER, PLACEMENT_OFFICER, ADMIN)
├── profile_completed
├── password_reset_token
├── password_reset_token_expiration
├── created_at
└── updated_at
```

#### `jobs`
Stores job postings created by employers.

```
jobs
├── id (PK)
├── employer_id (FK → users)
├── title
├── description
├── company_name
├── location
├── salary_range
├── required_skills
├── experience_required
├── status (ENUM: OPEN, CLOSED, DRAFT)
├── created_at
└── updated_at
```

#### `applications`
Tracks job applications submitted by students.

```
applications
├── id (PK)
├── job_id (FK → jobs)
├── student_id (FK → users)
├── status (ENUM: APPLIED, SHORTLISTED, REJECTED, ACCEPTED)
├── resume_url
├── notes
├── cover_letter
├── applied_at
└── updated_at
```

#### `notifications`
Stores system and transactional notifications.

```
notifications
├── id (PK)
├── user_id (FK → users)
├── message
├── type (INFO, SUCCESS, WARNING, ERROR)
├── is_read (BOOLEAN)
├── created_at
└── updated_at
```

#### `student_profiles`
Extended profile information for students.

```
student_profiles
├── id (PK)
├── user_id (FK → users, UNIQUE)
├── resume_url
├── skills
├── cgpa
├── branch
└── updated_at
```

#### `employer_profiles`
Extended profile information for employers.

```
employer_profiles
├── id (PK)
├── user_id (FK → users, UNIQUE)
├── company_name
├── industry
├── company_size
└── updated_at
```

#### `placement_profiles`
Extended profile information for placement officers.

```
placement_profiles
├── id (PK)
├── user_id (FK → users, UNIQUE)
├── department
└── updated_at
```

---

## 🔐 Authentication Flow

The application uses JWT (JSON Web Tokens) with HttpOnly cookies for secure authentication:

```
1. User Login
   │
   └─→ POST /api/auth/login
       ├─ Validate email & password
       ├─ Check against stored BCrypt hash
       └─ Generate JWT token (24-hour expiration)
           │
           ├─→ Create HttpOnly cookie
           │   (Secure, SameSite=Lax)
           │
           └─→ Return user data to client

2. Authenticated Requests
   │
   └─→ Client sends request with cookie
       │
       ├─→ AuthTokenFilter intercepts
       │
       ├─→ Extract JWT from cookie
       │
       ├─→ Validate token signature & expiration
       │
       ├─→ Load user details
       │
       └─→ Set SecurityContext
           │
           └─→ Process request with user context

3. Logout
   │
   └─→ POST /api/auth/logout
       ├─ Clear HttpOnly cookie
       ├─ Invalidate session
       └─ Clear SecurityContext
```

### Security Features

- **Token Storage**: Tokens stored in HttpOnly cookies (not accessible via JavaScript)
- **CSRF Protection**: Disabled for stateless JWT auth (can be enabled if needed)
- **CORS**: Configured to allow requests from frontend origins
- **Password Hashing**: BCrypt with 10 rounds
- **Token Expiration**: 24 hours (configurable)
- **Refresh Token**: Not implemented (frontend re-authenticates on expiration)

---

## 📡 API Endpoints

### Authentication Endpoints

```
POST   /api/auth/login              Login user (public)
POST   /api/auth/logout             Logout user
POST   /api/auth/register           Register new user (public)
POST   /api/auth/forgot-password    Request password reset
POST   /api/auth/reset-password     Reset password with token
```

### Job Endpoints

```
GET    /api/jobs                    List all jobs (public)
GET    /api/jobs/{id}               Get job details (public)
GET    /api/jobs/employer/{empId}   Get employer's jobs (employer)
POST   /api/jobs                    Create job (employer, admin)
PUT    /api/jobs/{id}               Update job (employer, admin)
DELETE /api/jobs/{id}               Delete job (employer, admin)
```

### Application Endpoints

```
GET    /api/applications/student/{id}    Get student applications
GET    /api/applications/job/{jobId}     Get job applications
GET    /api/applications/employer/{id}   Get employer applications
GET    /api/applications/employer/applications/{jobId}  Get specific job applications
POST   /api/applications                 Submit job application
PUT    /api/applications/employer/application/{id}/status  Update application status
```

### User Endpoints

```
GET    /api/users/profile/{email}   Get user profile (authenticated)
PUT    /api/users/profile/{id}      Update user profile (authenticated)
POST   /api/users                   Create user (admin only)
GET    /api/users                   List all users (admin only)
DELETE /api/users/{id}              Delete user (admin only)
```

### Notification Endpoints

```
GET    /api/notifications           Get user notifications (authenticated)
POST   /api/notifications           Create notification (admin only)
POST   /api/notifications/mark-read Mark all as read (authenticated)
POST   /api/notifications/{id}/mark-read  Mark single as read (authenticated)
```

### Dashboard Endpoints

```
GET    /api/admin/dashboard         Admin dashboard stats (admin only)
GET    /api/placement/dashboard     Placement officer stats (placement officer only)
```

### Utility Endpoints

```
GET    /api/health                  Health check (public)
POST   /api/contact                 Contact form submission (public)
```

---

## 🛡️ Security Configuration

### Spring Security Setup

```java
- Stateless session management (JWT-based)
- Method-level security (@PreAuthorize annotations)
- Role-based access control (ROLE_STUDENT, ROLE_EMPLOYER, etc.)
- CORS configuration with credentialsAllowed
- CSRF disabled (stateless API)
- JWT token filter before UsernamePasswordAuthenticationFilter
```

### Endpoint Security Rules

```
PUBLIC:
  - /api/auth/** (login, register, password reset)
  - /api/test/** (test endpoints)
  - /api/health
  - GET /api/jobs and /api/jobs/**
  - /uploads/** (resume files)

AUTHENTICATED:
  - /api/users/profile/**
  - /api/applications/** (with role-based filtering)
  - /api/notifications

ROLE-SPECIFIC:
  - /api/admin/** (requires ROLE_ADMIN)
  - /api/placement/** (requires ROLE_PLACEMENT_OFFICER)
  - /api/employer/** (requires ROLE_EMPLOYER)
```

---

## ⚡ Performance Optimization

### Caching Strategy

- **Caffeine Cache**: Local in-memory cache for frequently accessed data
- **Cache Eviction**: Dashboard data cached and evicted on entity updates
- **Configurable TTL**: Cache expiration times configured per data type

### Database Optimization

- **Batch Processing**: Hibernate batch insert/update enabled
- **Query Optimization**: Database indexes on frequently queried columns
- **Lazy Loading**: Relationships configured with appropriate fetch strategies
- **Connection Pooling**: HikariCP with optimized pool settings (max 20 connections)

### Rate Limiting

```
Global API: 100 requests per minute per IP
Sensitive Endpoints: 20 requests per minute per IP
- Login attempts
- Password reset
- Registration
```

---

## 🔔 Notification System

### Notification Types

- `APPLICATION_SUBMITTED`: Student notifies employer of application
- `APPLICATION_STATUS`: Employer notifies student of status change
- `SYSTEM_INFO`: Admin system messages

### Features

- **Real-Time Creation**: Notifications created immediately on events
- **Read Status Tracking**: Track which notifications user has read
- **Bulk Operations**: Mark all notifications as read
- **Duplicate Prevention**: 30-second window to prevent duplicate notifications

### Event Triggers

```
Notification created when:
- Student applies for job
- Application status changes
- Admin sends system message
- New job posted (configurable)
```

---

## 📤 File Upload Handling

### Resume Upload

- **Supported Formats**: PDF, DOC, DOCX, TXT
- **Max File Size**: 5MB
- **Storage Location**: `/uploads/resumes/`
- **File Naming**: `userId_timestamp_originalFilename`
- **Access**: Secured with authentication

### Validation

```java
- File type validation (MIME type checking)
- File size validation
- Virus scan (optional integration)
- Duplicate detection
```

---

## 🚨 Error Handling

### Global Exception Handler

All errors are caught and returned in a consistent format:

```json
{
  "error": "Error message",
  "status": 400,
  "timestamp": "2024-04-07T10:30:00Z",
  "path": "/api/endpoint"
}
```

### Common Error Codes

| Code | Message | Cause |
|------|---------|-------|
| 400 | Bad Request | Invalid input data |
| 401 | Unauthorized | Missing or invalid token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource already exists (duplicate email) |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Unexpected server error |

---

## 📊 Logging

### Log Configuration

- **Framework**: SLF4j with Logback
- **Log Levels**: 
  - Production: INFO
  - Development: DEBUG
- **Pattern**: `%d{yyyy-MM-dd HH:mm:ss} [%X{requestId}] %-5level %logger - %msg%n`

### Log Categories

```properties
# Spring Framework
logging.level.org.springframework=WARN

# Security
logging.level.org.springframework.security=WARN

# Database
logging.level.org.hibernate.SQL=INFO
logging.level.org.hibernate.orm.jdbc.bind=INFO

# Application
logging.level.com.placementpro.backend=INFO
```

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8.0+
- Git

### Installation

```bash
# Clone repository
git clone <repository-url>
cd PlacementPro_Backend

# Build project
mvn clean install

# Verify build
mvn verify
```

### Database Setup

```bash
# Create database (MySQL automatically creates with DDL auto)
CREATE DATABASE placementpro CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Or let Hibernate auto-create with:
spring.jpa.hibernate.ddl-auto=update
```

---

## ⚙️ Configuration

### Environment Variables

Create a `.env` file in the project root:

```env
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/placementpro
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your-256-bit-secret-key-minimum-32-characters

# Email Configuration (SMTP)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password

# Application Profiles
SPRING_PROFILES_ACTIVE=prod

# Server Port
SERVER_PORT=8080
```

### Application Properties

Key configuration properties in `application.properties`:

```properties
# Database
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

# Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# JWT
app.jwt.expiration-ms=86400000

# Rate Limiting
rate-limit.global-api.limit=100
rate-limit.sensitive-endpoint.limit=20

# Caching
spring.cache.type=caffeine
```

---

## 🏃 Running the Application

### Development Mode

```bash
# Run Spring Boot application
mvn spring-boot:run

# Or use IDE run configuration for faster startup
```

The application starts on `http://localhost:8080`

### Profile-Specific Configuration

```bash
# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Or set environment variable
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

### Build Production JAR

```bash
# Create executable JAR
mvn clean package

# Run JAR
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserControllerTest

# Run with coverage
mvn test jacoco:report
```

---

## 🚀 Deployment

### Local Deployment

```bash
# Build and run
mvn clean install
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### Docker Deployment

```dockerfile
FROM eclipse-temurin:21-jdk
COPY target/backend-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build image
docker build -t placementpro-backend .

# Run container
docker run -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/placementpro placementpro-backend
```

### Render.com Deployment

1. Connect GitHub repository to Render
2. Create new Web Service
3. Set build command: `mvn clean install`
4. Set start command: `java -jar target/backend-0.0.1-SNAPSHOT.jar`
5. Add environment variables
6. Deploy

### AWS Deployment (Elastic Beanstalk)

```bash
# Install EB CLI
pip install awsebcli

# Initialize
eb init -p java-21

# Create environment and deploy
eb create production-env
eb deploy
```

---

## 📝 Development Guidelines

### Code Style

- Follow Google Java Style Guide
- Use meaningful variable names
- Add JavaDoc for public methods
- Keep methods focused on single responsibility

### Creating New Endpoints

1. Create controller method in appropriate Controller
2. Add service method in Service layer
3. Add repository query in Repository (if needed)
4. Use appropriate HTTP method and status codes
5. Add @PreAuthorize for security
6. Document with request/response examples

### Adding New Entity

1. Create Entity class with @Entity and @Table
2. Add JPA relationships with proper fetch types
3. Create Repository extending JpaRepository
4. Create DTO for API responses
5. Add Service methods
6. Create Controller endpoints

---

## 🐛 Troubleshooting

### Database Connection Issues

```
Error: "Communications link failure"
Solution:
- Verify MySQL is running
- Check datasource URL and credentials
- Ensure database exists or ddl-auto=create
```

### JWT Token Issues

```
Error: "JWT signature does not match"
Solution:
- Verify JWT_SECRET is same as configured
- Check token hasn't expired (24 hours)
- Ensure token format is "Bearer <token>"
```

### CORS Issues

```
Error: "No 'Access-Control-Allow-Origin' header"
Solution:
- Verify frontend URL is in CORS allowed origins
- Check withCredentials=true on frontend
- Verify SecurityConfig CORS configuration
```

### Rate Limiting

```
Error: "429 Too Many Requests"
Solution:
- Check if endpoint is rate-limited
- Wait for rate limit window to reset
- Increase limits in application.properties if needed
```

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [JWT Introduction](https://jwt.io)
- [Hibernate Documentation](https://hibernate.org)

---

## 🤝 Contributing

1. Create feature branch: `git checkout -b feature/new-feature`
2. Follow code style guidelines
3. Write tests for new functionality
4. Commit with clear messages: `git commit -m "Add new feature"`
5. Push to branch: `git push origin feature/new-feature`
6. Create pull request

---

## 🆘 Support

For issues, questions, or contributions, please:
- Create an issue in the repository
- Contact the development team
- Review existing documentation


