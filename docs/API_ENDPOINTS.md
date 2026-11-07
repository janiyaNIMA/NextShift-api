# NextShift / UniShift — API Endpoints Specification

Base URL: `http://localhost:8080`

All protected endpoints require an `Authorization: Bearer <JWT_TOKEN>` header.

---

## 1. Public & Utility Endpoints

### 1.1 Health Check
- **Method**: `GET`
- **Path**: `/health`
- **Access**: Public
- **Response**: `200 OK`
```json
{
  "status": "UP",
  "service": "unishift-backend",
  "timestamp": "2026-11-01T18:00:00Z"
}
```

---

## 2. Authentication Endpoints

### 2.1 Register User
- **Method**: `POST`
- **Path**: `/api/v1/auth/register`
- **Access**: Public
- **Request Body**:
```json
{
  "name": "Jane Doe",
  "email": "jane@student.uni.edu",
  "password": "SecurePassword123!",
  "role": "STUDENT"
}
```
*Note: `role` must be `STUDENT` or `EMPLOYER`. `ADMIN` is rejected with `400 Bad Request`.*
- **Response**: `201 Created`
```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane@student.uni.edu",
    "role": "STUDENT",
    "active": true,
    "approved": true,
    "createdAt": "2026-11-01T18:00:00Z"
  }
}
```

### 2.2 User Login
- **Method**: `POST`
- **Path**: `/api/v1/auth/login`
- **Access**: Public
- **Request Body**:
```json
{
  "email": "jane@student.uni.edu",
  "password": "SecurePassword123!"
}
```
- **Response**: `200 OK`
```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane@student.uni.edu",
    "role": "STUDENT",
    "active": true,
    "approved": true,
    "createdAt": "2026-11-01T18:00:00Z"
  }
}
```

### 2.3 Current User Profile
- **Method**: `GET`
- **Path**: `/api/v1/auth/me`
- **Access**: Authenticated (`STUDENT`, `EMPLOYER`, `ADMIN`)
- **Response**: `200 OK`
```json
{
  "id": 1,
  "name": "Jane Doe",
  "email": "jane@student.uni.edu",
  "role": "STUDENT",
  "active": true,
  "approved": true,
  "createdAt": "2026-11-01T18:00:00Z"
}
```

---

## 3. Job Vacancy Endpoints

### 3.1 Public Job Feed (Recommendation Strategy V0)
- **Method**: `GET`
- **Path**: `/api/v1/jobs?category=Education&location=Campus&page=0&size=20`
- **Access**: Public
- **Query Parameters**:
  - `category` (optional, string)
  - `location` (optional, string)
  - `page` (optional, default `0`)
  - `size` (optional, default `20`)
- **Response**: `200 OK`
```json
{
  "content": [
    {
      "id": 10,
      "employerId": 2,
      "employerName": "Campus Bookstore",
      "title": "Inventory Assistant",
      "description": "Stocking books and scanning barcodes",
      "category": "Retail",
      "location": "North Campus Bookstore",
      "payAmount": 22.50,
      "payUnit": "HOURLY",
      "startsAt": "2026-11-05T09:00:00Z",
      "endsAt": "2026-11-05T15:00:00Z",
      "capacity": 3,
      "confirmedBookings": 1,
      "remainingCapacity": 2,
      "status": "PUBLISHED",
      "createdAt": "2026-11-01T10:00:00Z",
      "publishedAt": "2026-11-01T12:00:00Z"
    }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 3.2 Job Details
- **Method**: `GET`
- **Path**: `/api/v1/jobs/{id}`
- **Access**: Public
- **Response**: `200 OK` (or `404 Not Found` if job is not published, past-dated, or nonexistent)

### 3.3 Create Job Vacancy
- **Method**: `POST`
- **Path**: `/api/v1/jobs`
- **Access**: Approved Employer (`ROLE_EMPLOYER`, `approved: true`)
- **Request Body**:
```json
{
  "title": "Barista Shift",
  "description": "Prepare espresso drinks during morning rush",
  "category": "Hospitality",
  "location": "Student Union Cafe",
  "payAmount": 20.00,
  "payUnit": "HOURLY",
  "startsAt": "2026-11-10T08:00:00Z",
  "endsAt": "2026-11-10T12:00:00Z",
  "capacity": 2
}
```
- **Response**: `201 Created` (initial status is `PENDING`)

---

## 4. Employer Management Endpoints

### 4.1 Employer's Own Vacancies
- **Method**: `GET`
- **Path**: `/api/v1/employer/jobs`
- **Access**: Employer (`ROLE_EMPLOYER`)
- **Response**: `200 OK` (list of all vacancies created by authenticated employer regardless of status)

### 4.2 Bookings For Employer Vacancy
- **Method**: `GET`
- **Path**: `/api/v1/employer/jobs/{id}/bookings`
- **Access**: Employer Owner (`ROLE_EMPLOYER` and vacancy belongs to caller)
- **Response**: `200 OK`
```json
[
  {
    "id": 1,
    "jobId": 10,
    "jobTitle": "Inventory Assistant",
    "jobLocation": "North Campus Bookstore",
    "studentId": 5,
    "studentName": "Alice Johnson",
    "studentEmail": "alice@student.uni.edu",
    "status": "CONFIRMED",
    "createdAt": "2026-11-01T14:30:00Z"
  }
]
```

---

## 5. Booking Endpoints

### 5.1 Book a Job Vacancy
- **Method**: `POST`
- **Path**: `/api/v1/jobs/{id}/bookings`
- **Access**: Student (`ROLE_STUDENT`)
- **Response**: `201 Created`
```json
{
  "id": 1,
  "jobId": 10,
  "jobTitle": "Inventory Assistant",
  "jobLocation": "North Campus Bookstore",
  "studentId": 5,
  "studentName": "Alice Johnson",
  "studentEmail": "alice@student.uni.edu",
  "status": "CONFIRMED",
  "createdAt": "2026-11-01T14:30:00Z"
}
```
- **Error Responses**:
  - `409 Conflict`: Duplicate booking by same student OR job capacity reached.
  - `403 Forbidden`: Caller is not a student.
  - `404 Not Found`: Job does not exist or is not eligible.

### 5.2 Student's Own Bookings
- **Method**: `GET`
- **Path**: `/api/v1/bookings/me`
- **Access**: Student (`ROLE_STUDENT`)
- **Response**: `200 OK` (list of bookings belonging to authenticated student)

---

## 6. Admin Endpoints

### 6.1 List Pending Employers
- **Method**: `GET`
- **Path**: `/api/v1/admin/employers/pending`
- **Access**: Admin (`ROLE_ADMIN`)
- **Response**: `200 OK` (list of `UserResponse` with `role: EMPLOYER` and `approved: false`)

### 6.2 Approve Employer
- **Method**: `PATCH`
- **Path**: `/api/v1/admin/employers/{id}/approve`
- **Access**: Admin (`ROLE_ADMIN`)
- **Response**: `200 OK` (returns updated `UserResponse` with `approved: true`)

### 6.3 List Pending Job Vacancies
- **Method**: `GET`
- **Path**: `/api/v1/admin/jobs/pending`
- **Access**: Admin (`ROLE_ADMIN`)
- **Response**: `200 OK` (list of `JobResponse` with `status: PENDING`)

### 6.4 Approve Job Vacancy
- **Method**: `PATCH`
- **Path**: `/api/v1/admin/jobs/{id}/approve`
- **Access**: Admin (`ROLE_ADMIN`)
- **Response**: `200 OK` (transitions job status to `PUBLISHED` and sets `publishedAt = Instant.now()`)
