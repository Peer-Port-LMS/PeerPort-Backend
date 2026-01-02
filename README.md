# PeerPort Backend

A Spring Boot-based Learning Management System (LMS) backend that provides RESTful APIs for course management, assignments, announcements, content delivery, and user authentication.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Database Setup](#database-setup)
- [OAuth2 Configuration](#oauth2-configuration)
- [File Upload Configuration](#file-upload-configuration)
- [Running the Application](#running-the-application)
- [Architecture Overview](#architecture-overview)
- [API Endpoints](#api-endpoints)
- [TODO](#todo)
- [Coming Soon](#coming-soon)

## Prerequisites

- **Java Development Kit (JDK) 25** (or later versions)
- **Maven 3.x**
- **PostgreSQL** (for production database)
- **GitHub OAuth App** (for authentication)

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Peer-Port-LMS/PeerPort-Backend.git
cd backend
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
CREATE DATABASE pp_prod;
```

3. **Configure Database Credentials**

Edit `src/main/resources/application.properties` and update the following properties:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/pp_prod
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
```

Replace:
- `localhost:5432` with your PostgreSQL server host and port if different
- `pp_prod` with your database name
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
   - **Homepage URL**: `http://localhost:8080`
   - **Authorization callback URL**: `http://localhost:8080/login/oauth2/code/github`
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

### CORS Configuration

The application is configured to allow requests from `http://localhost:5173` (frontend dev server). If your frontend runs on a different port, update the CORS configuration in:

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
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

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
│   │   ├── AuthController.java          # Authentication endpoints
│   │   ├── CourseController.java        # Course management
│   │   ├── AssignmentController.java    # Assignment management
│   │   ├── AnnouncementController.java  # Announcement management
│   │   ├── ContentController.java       # Content management
│   │   ├── FileController.java          # File serving
│   │   ├── EnrollmentController.java    # Enrollment management
│   │   └── UserController.java          # User management
│   ├── model/                           # JPA entity models
│   │   ├── CourseModel.java             # Course entity
│   │   ├── UserModel.java               # User entity with OAuth2
│   │   ├── AssignmentModel.java         # Assignment entity
│   │   ├── AnnouncementModel.java       # Announcement entity
│   │   ├── ContentModel.java            # Hierarchical content entity
│   │   ├── FileModel.java               # File metadata entity
│   │   ├── EnrollmentModel.java         # Student enrollment entity
│   │   └── RoleModel.java               # User role enum
│   ├── dto/                             # Data Transfer Objects
│   ├── service/                         # Business logic layer
│   ├── database/                        # JPA repositories
│   ├── validation/                      # Custom validators
│   │   └── CourseValidator.java         # Date validation
│   └── utils/                           # Utility classes
├── src/main/resources/
│   ├── application.properties           # Application configuration
│   └── static/
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
- **AuthController**: OAuth2 authentication flow
- **CourseController**: CRUD operations for courses with image upload
- **AssignmentController**: Assignment lifecycle management
- **AnnouncementController**: Course announcements
- **ContentController**: Hierarchical content structure
- **FileController**: File download/serving
- **EnrollmentController**: Student course enrollments
- **UserController**: User management

#### Models
JPA entities that map to database tables:
- **CourseModel**: Course information with dates, instructors, and relationships
- **UserModel**: User accounts with OAuth2 provider data and roles
- **AssignmentModel**: Assignments with due dates and visibility
- **AnnouncementModel**: Course announcements with timestamps
- **ContentModel**: Hierarchical content (supports parent-child relationships)
- **FileModel**: File metadata and storage paths
- **EnrollmentModel**: Student-course enrollment records

#### Database Layer
Spring Data JPA repositories for database access with automatic query generation and custom query methods.

#### Validation
Custom validators and Jakarta validation annotations ensure data integrity:
- **CourseValidator**: Validates course start/end dates
- Bean validation annotations: `@NotNull`, `@Size`, `@Email`, etc.

#### Security
Role-based access control (RBAC) with three roles:
- **STUDENT**: Basic access to enrolled courses
- **INSTRUCTOR**: Can create and manage courses, assignments, and announcements
- **ADMIN**: Full system access

### Database Relationships

- **User ↔ Course**: Many-to-Many (instructors can teach multiple courses)
- **User → Enrollment**: One-to-Many (students can enroll in multiple courses)
- **Course → Assignment**: One-to-Many
- **Course → Announcement**: One-to-Many
- **Course → Content**: One-to-Many
- **Course → File**: One-to-One (course image)
- **Content → File**: One-to-Many (attachments)
- **Content → Content**: Self-referential (hierarchical structure)

## API Endpoints

### Authentication
- `GET /auth/user` - Get authenticated user information
- `GET /auth/login` - Initiate OAuth2 login flow

### Courses
- `GET /courses` - Get all courses (Admin only)
- `GET /courses/{id}` - Get course by ID with details
- `POST /courses` - Create new course (Admin/Instructor)
- `PUT /courses/{id}` - Update course (Admin/Instructor)
- `DELETE /courses/{id}` - Delete course (Admin/Instructor)

**Authorization**: STUDENT, INSTRUCTOR, ADMIN

### Assignments
- `GET /assignments` - Get all assignments with course info
- `GET /assignments/{id}` - Get assignment by ID
- `POST /assignments` - Create new assignment
- `PUT /assignments/{id}` - Update assignment
- `PATCH /assignments/{id}` - Partial update
- `DELETE /assignments/{id}` - Delete assignment

### Announcements
- `GET /announcements` - Get all announcements
- `GET /announcements/{id}` - Get announcement by ID
- `POST /announcements` - Create announcement
- `PUT /announcements/{id}` - Update announcement
- `PATCH /announcements/{id}` - Partial update
- `DELETE /announcements/{id}` - Delete announcement

### Content
- `GET /content` - Get all content with hierarchy
- `GET /content/{id}` - Get content by ID with details
- `POST /content` - Create content
- `PUT /content/{id}` - Update content
- `PATCH /content/{id}` - Partial update
- `DELETE /content/{id}` - Delete content

### Files
- `GET /files/{id}` - Download/view file by ID

### Enrollments
- `GET /enrollments` - Get all enrollments
- `GET /enrollments/{id}` - Get enrollment by ID
- `POST /enrollments` - Create enrollment
- `PUT /enrollments/{id}` - Update enrollment
- `DELETE /enrollments/{id}` - Delete enrollment

**Authorization**: ADMIN, INSTRUCTOR only

### Users
User management endpoints

**Authorization**: STUDENT, INSTRUCTOR, ADMIN

## TODO

### High Priority
- [ ] Enable CSRF protection in SecurityConfig (currently disabled)
- [ ] Implement assignment submission system
- [ ] Add grading functionality
- [ ] Implement file permissions and access control

### Features
- [ ] Add discussion forums
- [ ] Implement email notifications
- [ ] Create analytics dashboard
- [ ] Add student progress tracking
- [ ] Implement grade book
- [ ] Add rubrics for assignments
- [ ] Support for quizzes and online tests
- [ ] Rich text editor for content
- [ ] Video embedding support

### Improvements
- [ ] Add rate limiting
- [ ] Improve input sanitization
- [ ] Enhanced file type validation
- [ ] Bulk user import functionality
- [ ] SCORM content support

### Testing & Documentation
- [ ] Add unit tests
- [ ] Add integration tests
- [ ] Add end-to-end tests
- [ ] Create user guides
- [ ] Document deployment procedures

## Coming Soon

### Swagger/OpenAPI Documentation
Interactive API documentation will be available soon, allowing you to:
- Explore all API endpoints
- Test API calls directly from the browser
- View request/response schemas
- Generate API client code

---

**Built using Spring Boot**
