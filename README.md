# PeerPort Backend

A Spring Boot-based Learning Management System (LMS) backend that provides RESTful APIs for the frontend

## Table of Contents

- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Database Setup](#database-setup)
- [OAuth2 Configuration](#oauth2-configuration)
- [File Upload Configuration](#file-upload-configuration)
- [Running the Application](#running-the-application)
- [Architecture Overview](#architecture-overview)
- [Roadmap & Version Planning](#roadmap--version-planning)
  - [MVP](#mvp-minimum-viable-product)
  - [Version 1.0](#version-10-post-mvp)
  - [Version 2.0](#version-20-future-enhancement)
  - [Future Considerations](#future-considerations)

## Prerequisites

- **Java Development Kit (JDK) 25** (or later versions)
- **Maven 3.x**
- **PostgreSQL** (for production database)
- **GitHub OAuth App** (for authentication)

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Peer-Port-LMS/PeerPort-Backend.git
cd PeerPort-Backend
```

### 2. Install Dependencies

```bash
mvn clean install
```

This will download all required dependencies defined in `pom.xml`.

## Database Setup

### PostgreSQL Configuration

1. **Install PostgreSQL** if not already installed

2. **Create a Database**

```sql
CREATE DATABASE YOUR_DATABASE_NAME;
```

3. **Configure Database Credentials**

Copy `src/main/resources/application.properties.example` and rename it to: `src/main/resources/application.properties` then update the following properties:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://YOUR_DATABASE_URL/YOUR_DATABASE_NAME
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
```

Replace:
- `YOUR_DATABASE_URL` with your PostgreSQL server host and port if different
- `YOUR_DATABASE_NAME` with your database name
- `YOUR_DB_USERNAME` with your PostgreSQL username
- `YOUR_DB_PASSWORD` with your PostgreSQL password

### Database Schema

The application uses Hibernate with `spring.jpa.hibernate.ddl-auto=update`, which automatically creates and updates database tables based on your entity models. No manual schema creation is required.

## OAuth2 Configuration

### GitHub OAuth App Setup

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click **New OAuth App**
3. Fill in the application details:
   - **Application name**: PeerPort LMS (or your preferred name)
   - **Homepage URL**: `YOUR_FRONTEND_URL`
   - **Authorization callback URL**: `http://WHERE_YOUR_HOSTING_THIS/login/oauth2/code/github`
4. Click **Register application**
5. Copy the **Client ID** and generate a **Client Secret**

### Configure OAuth2 Credentials

Edit `src/main/resources/application.properties` and update:

```properties
# OAuth2 GitHub Configuration
spring.security.oauth2.client.registration.github.client-id=YOUR_GITHUB_CLIENT_ID
spring.security.oauth2.client.registration.github.client-secret=YOUR_GITHUB_CLIENT_SECRET
```

Replace:
- `YOUR_GITHUB_CLIENT_ID` with your GitHub OAuth App Client ID
- `YOUR_GITHUB_CLIENT_SECRET` with your GitHub OAuth App Client Secret

### CORS Configuration <small>(Implemented - needs environment hardening)</small>

The application is configured to allow requests from `http://localhost:5173` (frontend dev server). For staging/production, externalize and restrict origins per environment.

```properties
# CORS Configuration
cors.allowed.origins=http://localhost:5173
```

## File Upload Configuration

### Setup Upload Directory

1. **Create the upload directory** (or use existing directory):

```bash
mkdir -p uploads/courses
```

2. **Configure upload path** in `src/main/resources/application.properties`:

```properties
# File Upload Configuration
file.upload-dir=./uploads
```

Update the path if you want to use a different location. The path can be relative or absolute.

### Upload Limits

The application has the following upload limits:

```properties
file.upload-size-limit=5242880
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```
5MB: 5242880

## Running the Application

### Development Mode

```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

### Building for Production

```bash
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

## Architecture Overview

### Project Structure

```
backend/
├── src/main/java/peerport/backend/
│   ├── BackendApplication.java          # Main Spring Boot application
│   ├── config/
│   │   └── SecurityConfig.java          # Security & CORS configuration
│   ├── controllers/                     # REST API endpoints
|   ├── exceptions/                      # Custom exceptions
│   ├── model/                           # JPA entity models
│   ├── dto/                             # Data Transfer Objects
│   ├── service/                         # Business logic layer
│   ├── database/                        # JPA repositories
│   └── validation/                      # Custom validators
├── src/main/resources/
|   ├── META-INF                         # Metadata about fields in .properties
│   └── application.properties           # Application configuration
└── pom.xml                              # Maven dependencies
```

### Technology Stack

- **Spring Boot 4.0.1** - Application framework
- **Spring Data JPA** - Database ORM
- **Spring Security** - Authentication & authorization
- **Spring OAuth2 Client** - GitHub OAuth2 integration
- **PostgreSQL** - Production database
- **H2 Database** - Development/testing support
- **Jakarta Validation** - Input validation
- **Thymeleaf** - Template engine

### Key Components

#### Controllers
REST API endpoints that handle HTTP requests and responses. Each controller manages a specific domain:
- **AuthController**: OAuth2 authentication flow with provider redirects (GitHub, etc.)
- **CourseController**: Full CRUD operations for courses including image uploads, course filtering, and instructor management
- **AssignmentController**: Complete assignment lifecycle (create, read, update, delete) with visibility controls and due date management
- **AssignmentSubmissionController**: Assignment submission creation/retrieval/deletion with multipart file upload support
- **AnnouncementController**: Course announcements with creation, updates, and deletion capabilities
- **ContentController**: Hierarchical content structure management with parent-child relationships
- **GradeController**: Grade CRUD and linking grades to assignment submissions
- **FileController**: File download/serving with secure file access and metadata handling
- **EnrollmentController**: Student course enrollment management and enrollment records
- **UserController**: User profile management and user data retrieval

#### Models
JPA entities that map to database tables:
- **CourseModel**: Course information including title, description, start/end dates, image references, and many-to-many instructor relationships
- **UserModel**: User accounts with OAuth2 provider authentication, profile pictures, ID numbers, and role-based access control
- **AssignmentModel**: Assignments with names, descriptions, due dates, point values, and visibility flags
- **AssignmentSubmissionModel**: Student assignment submissions with submission metadata, attached files, and optional grade relationship
- **GradeModel**: Submission grading data including obtained grade, maximum grade, feedback, and graded timestamps
- **AnnouncementModel**: Course announcements with titles, content, creation/update timestamps, and course associations
- **ContentModel**: Hierarchical content structure supporting parent-child relationships for organizing course materials
- **FileModel**: File metadata and storage paths with secure file serving and cleanup on deletion
- **EnrollmentModel**: Student-course enrollment records with enrollment dates and completion tracking
- **RoleModel**: Enum-based user roles (STUDENT, INSTRUCTOR, ADMIN) for role-based access control

#### Database Layer
Spring Data JPA repositories for database access with automatic query generation and custom query methods.

#### Validation
Custom validators and Jakarta validation annotations ensure data integrity:
- **ValidEndDateAfterStartDate**: Validates start/end dates
- Bean validation annotations: `@NotNull`, `@Size`, `@Email`, etc.

#### Security
Role-based access control (RBAC) with three roles:
- **STUDENT**: Basic access to enrolled courses
- **INSTRUCTOR**: Can create and manage courses, assignments, and announcements
- **ADMIN**: Full system access

Current status: core RBAC is implemented, while security hardening is still in progress (CSRF enablement strategy and endpoint authorization coverage checks).

### Database Relationships

- **User ↔ Course**: Many-to-Many (via CourseInstructors join table; instructors can teach multiple courses)
- **User → Enrollment**: One-to-Many (users have multiple enrollment records for different courses)
- **Enrollment ← User & Course**: Composite relationship (enrollment acts as a join table with metadata like dateEnrolled and dateCompleted)
- **Course → Assignment**: One-to-Many (each course has multiple assignments)
- **Assignment → AssignmentSubmission**: One-to-Many (each assignment can have multiple student submissions)
- **AssignmentSubmission → Grade**: One-to-One (a submission can have a single grade record)
- **Course → Announcement**: One-to-Many (each course has multiple announcements)
- **Course → Content**: One-to-Many (each course has hierarchical content)
- **Content → Content**: Self-referential Many-to-One (supports hierarchical parent-child structure)
- **Content → File**: One-to-Many (content can have multiple file attachments)
- **Course → File**: One-to-One (course image reference)

**Authorization**: ADMIN, INSTRUCTOR only

### Users
User management endpoints

**Authorization**: STUDENT, INSTRUCTOR, ADMIN

## Roadmap & Version Planning
<img src="https://img.shields.io/badge/status-complete-success" alt="Complete">
<img src="https://img.shields.io/badge/status-in%20progress-yellow" alt="In Progress">
<img src="https://img.shields.io/badge/status-not%20started-red" alt="Not Started">

---

## MVP (Minimum Viable Product)

The MVP focuses on core LMS functionality: course management, basic content delivery, assignments, and announcements with essential grading support.

### Core Features - MVP

<details>
<summary>Implement Authentication & Session Flows <img src="https://img.shields.io/badge/status-in%20progress-yellow" alt="In Progress"></summary>

- [x] OAuth2 login flow implemented
- [x] AuthController endpoints implemented (`/auth/login`, `/auth/me`)
- [x] Service-level user bootstrap implemented
- [ ] Verify unauthorized/session-expired behavior end-to-end
- [ ] Add integration tests for auth flows
- [ ] Add javaDoc comments
</details>

<details>
<summary>Implement User Management <img src="https://img.shields.io/badge/status-in%20progress-yellow" alt="In Progress"></summary>

- [x] Model implemented
- [x] DTO implemented
- [x] Service implemented
- [x] Controller implemented
- [ ] Verify role-boundary functionality end-to-end
- [x] Add error messages
- [x] Add javaDoc comments
</details>

<details>
<summary>Implement Enrollment Management <img src="https://img.shields.io/badge/status-in%20progress-yellow" alt="In Progress"></summary>

- [x] Model implemented
- [x] DTO implemented
- [x] Service implemented
- [x] Controller implemented
- [ ] Verify functionality
- [x] Add error messages
- [x] Add javaDoc comments
</details>

<details>
<summary>Implement Courses <img src="https://img.shields.io/badge/status-in%20progress-yellow" alt="In Progress"></summary>

- [x] Model implemented
- [x] DTO implemented
- [x] Service implemented
- [x] Controller implemented
- [ ] Verify functionality
- [x] Add error messages
- [x] Add javaDoc comments
</details>

<details>
<summary>Implement Announcements <img src="https://img.shields.io/badge/status-in%20progress-yellow" alt="In Progress"></summary>

- [x] Model implemented
- [x] DTO implemented
- [x] Service implemented
- [x] Controller implemented
- [x] File attachment implemented
- [ ] Verify functionality
- [x] Add error messages
- [x] Add javaDoc comments
</details>

<details>
<summary>Implement Content <img src="https://img.shields.io/badge/status-in%20progress-yellow" alt="In Progress"></summary>

- [x] Model implemented
- [x] DTO implemented
- [x] Service implemented
- [x] Controller implemented
- [x] File attachment implemented
- [ ] Verify functionality
- [x] Add error messages
- [x] Add javaDoc comments
</details>

<details>
<summary>Implement Assignments <img src="https://img.shields.io/badge/status-in%20progress-yellow" alt="In Progress"></summary>

- [x] Model implemented
- [x] DTO implemented
- [x] Service implemented
- [x] Controller implemented
- [x] File attachment implemented
- [ ] Verify functionality
- [x] Add error messages
- [x] Add javaDoc comments
</details>

<details>
<summary>Implement Assignment Submissions <img src="https://img.shields.io/badge/status-in%20progress-yellow" alt="In Progress"></summary>

- [x] Model implemented
- [x] DTO implemented
- [x] Service implemented
- [x] Controller implemented
- [x] File attachment implemented
- [x] Add unit tests for assignment-scoped submissions and grade retrieval helpers
- [ ] Verify functionality
- [x] Add error messages
- [x] Add javaDoc comments
</details>

<details>
<summary>Implement Basic Grading <img src="https://img.shields.io/badge/status-in%20progress-yellow" alt="In Progress"></summary>

- [x] Grade Model implemented
- [x] Grade DTO implemented
- [x] Grade Service implemented
- [x] Grade Controller implemented
- [x] Add error messages
- [x] Add unit tests (service + model mapping)
- [ ] Verify functionality end-to-end
- [ ] Add javaDoc comments
</details>

<details>
<summary>Testing & Documentation - MVP <img src="https://img.shields.io/badge/status-in%20progress-yellow" alt="In Progress"></summary>

- [ ] Swagger/OpenAPI implemented
- [x] Add unit tests for core features
- [x] Add unit tests for assignment submission grade helper methods
- [ ] Add integration tests
- [ ] Create basic user guides
- [ ] Document API endpoints
</details>

### MVP Priorities
- [x] Complete basic grading workflow (scores + feedback + visibility)
- [ ] Finalize testing suite for MVP features (focus: integration tests for auth and authorization boundaries)
- [ ] Complete API documentation (OpenAPI/Swagger + endpoint usage examples)
- [ ] Perform security review before launch
- [ ] Enable CSRF protection strategy in SecurityConfig for session-based auth
- [x] Implement file permissions and access control
- [ ] Harden SecurityConfig endpoint coverage for announcements, submissions, and file routes
- [ ] Externalize `cors.allowed.origins` per environment (dev/staging/prod)

---

## Version 1.0 (Post-MVP)

Enhanced features and improved user experience after MVP release.

### Version 1.0 Features

<details>
<summary>Advanced Grading System <img src="https://img.shields.io/badge/status-not%20started-red" alt="Not Started"></summary>

- [ ] Implement grade book
- [ ] Add rubrics for assignments
- [ ] Support for weighted grading
- [ ] Grade distribution analytics
</details>

<details>
<summary>Content Enhancements <img src="https://img.shields.io/badge/status-not%20started-red" alt="Not Started"></summary>

- [ ] Rich text editor for content
- [ ] Support for quizzes and online tests
- [ ] SCORM content support
- [ ] Media embedding capabilities
</details>

<details>
<summary>Communication Features <img src="https://img.shields.io/badge/status-not%20started-red" alt="Not Started"></summary>

- [ ] Add discussion forums
- [ ] Implement email notifications
</details>

<details>
<summary>Testing & Documentation - v1.0 <img src="https://img.shields.io/badge/status-not%20started-red" alt="Not Started"></summary>

- [ ] Add end-to-end tests
- [ ] Comprehensive API documentation
- [ ] Create deployment guides
- [ ] Performance testing and optimization
</details>

<details>
<summary>API Consistency & Platform Hardening <img src="https://img.shields.io/badge/status-not%20started-red" alt="Not Started"></summary>

- [ ] Normalize update/delete endpoint paths and HTTP verb consistency
- [ ] Expand upload security controls (content-type allowlist for non-image uploads)
- [ ] Add security event observability (auth failures, forbidden access, file access audit trail)
</details>

---

## Version 2.0 (Future Enhancement)

Advanced features and system optimization.

### Version 2.0 Features

<details>
<summary>Analytics & Tracking <img src="https://img.shields.io/badge/status-not%20started-red" alt="Not Started"></summary>

- [ ] Create analytics dashboard
- [ ] Add student progress tracking
- [ ] Implement learning analytics
- [ ] Generate progress reports
</details>

<details>
<summary>Administration Features <img src="https://img.shields.io/badge/status-not%20started-red" alt="Not Started"></summary>

- [ ] Bulk user import functionality
- [ ] Advanced user management
- [ ] Custom role creation
- [ ] Audit logging
</details>

<details>
<summary>Performance & Security <img src="https://img.shields.io/badge/status-not%20started-red" alt="Not Started"></summary>

- [ ] Add rate limiting
- [ ] Implement caching strategies
- [ ] Enhanced input sanitization
- [ ] File storage migration (S3 buckets)
- [ ] Advanced file type validation
</details>

---

## Future Considerations

<details>
<summary>Advanced Features (Backlog)</summary>

- [ ] Peer review system
- [ ] Plagiarism detection integration
- [ ] Mobile app support
- [ ] Single Sign-On (SSO) integration
- [ ] Advanced calendar/scheduling
</details>

