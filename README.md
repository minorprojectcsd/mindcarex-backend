# 🧠 MindCareX Backend

> Mental Health Telemedicine Platform - Spring Boot REST API + WebSocket Server

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [WebSocket Guide](#websocket-guide)
- [Email System](#email-system)
- [Security](#security)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

MindCareX Backend is a comprehensive REST API and WebSocket server powering a mental health telemedicine platform. It enables secure video consultations between doctors and patients with real-time chat, email notifications, and session management.

**Live API:** https://mindcarex-backend.onrender.com  
**Health Check:** https://mindcarex-backend.onrender.com/health  
**Frontend:** https://mindcarex.vercel.app

---

## ✨ Features

### ✅ Core Features
- **User Management** - Separate roles (Doctor, Patient, Admin) with comprehensive profiles
- **Appointment System** - Book, view, cancel appointments with status tracking
- **Session Management** - Start/end video sessions with duration tracking
- **Real-time Chat** - WebSocket-based messaging with 24-hour retention
- **WebRTC Signaling** - STOMP over WebSocket for peer-to-peer video/audio
- **Email Notifications** - Confirmations, reminders, summaries, guardian alerts
- **Scheduled Tasks** - Automated 10-minute appointment reminders

### 🔒 Security Features
- JWT Authentication (stateless)
- Role-Based Access Control
- BCrypt Password Hashing
- CORS Protection
- HTTPS/WSS Encryption

---

## 🛠️ Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.5.9 |
| Language | Java | 21 |
| Security | Spring Security + JWT | 6.x |
| Database (SQL) | PostgreSQL | 16 |
| Database (NoSQL) | MongoDB | 7.0 |
| Email | JavaMailSender + Thymeleaf | - |
| WebSocket | Spring WebSocket + STOMP | - |
| Deployment | Render (Docker) | - |

---

## 📦 Prerequisites

- **Java 21** - [Download](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **PostgreSQL** - [Neon Account](https://neon.tech) (free cloud) or local
- **MongoDB** - [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) (free) or local
- **Email Service** - [Brevo Account](https://www.brevo.com) ✅ **RECOMMENDED** or Gmail
- **Git** - [Download](https://git-scm.com/downloads)

---

## 🚀 Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/mindcarex-backend.git
cd mindcarex-backend
```

### 2. Set Up Databases

#### PostgreSQL (Neon - Recommended)

1. Sign up at [neon.tech](https://neon.tech)
2. Create project → Copy connection string
3. Convert to JDBC:
   ```
   jdbc:postgresql://ep-xxx.aws.neon.tech/neondb?sslmode=require
   ```

#### MongoDB (Atlas - Recommended)

1. Sign up at [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas)
2. Create M0 Free cluster
3. Create user: `mindcarex_user` / `mindcarex@123`
4. Whitelist IP: `0.0.0.0/0`
5. Get connection string & **URL-encode password** (`@` → `%40`):
   ```
   mongodb+srv://mindcarex_user:mindcarex%40123@cluster.mongodb.net/mental_health_chat?retryWrites=true&w=majority&authSource=admin
   ```

6. **Create TTL Index** (MongoDB Compass or Shell):
   ```javascript
   use mental_health_chat;
   
   // Auto-delete messages after 24 hours
   db.chat_messages.createIndex(
     { "timestamp": 1 },
     { expireAfterSeconds: 86400 }
   );
   
   // Performance index
   db.chat_messages.createIndex(
     { "sessionId": 1, "timestamp": 1 }
   );
   ```

### 3. Set Up Email Service (Brevo)

#### ✅ Option 1: Brevo SMTP (Recommended)

1. Sign up at [brevo.com](https://www.brevo.com)
2. Verify your email
3. Go to: **Settings → SMTP & API**
4. Click **Create a new SMTP key**
5. Name it: "MindCareX Backend"
6. Copy the SMTP key

**Configuration:**
```bash
EMAIL_HOST=smtp-relay.brevo.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@example.com
EMAIL_PASSWORD=your-smtp-key-here
```

**Free Tier:** 300 emails/day

#### Option 2: Gmail SMTP

1. Enable 2FA: https://myaccount.google.com/security
2. Create App Password:
   - Search "App passwords"
   - Select: Mail → Other (Custom) → "MindCareX"
   - Copy 16-char password (remove spaces)

**Configuration:**
```bash
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-16-char-app-password
```

**Free Tier:** 500 emails/day

### 4. Configure Environment

Create `.env` file in project root:

```bash
# Database
DATABASE_URL=jdbc:postgresql://ep-xxx.aws.neon.tech/neondb?sslmode=require
DATABASE_USER=neondb_owner
DATABASE_PASSWORD=npg_xxxxx

# MongoDB
MONGODB_URI=mongodb+srv://mindcarex_user:mindcarex%40123@cluster.mongodb.net/mental_health_chat?retryWrites=true&w=majority&authSource=admin

# JWT (use strong secret, 32+ chars)
JWT_SECRET=your-super-secret-key-minimum-32-characters-long

# Email - BREVO (Recommended)
EMAIL_HOST=smtp-relay.brevo.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@example.com
EMAIL_PASSWORD=your-brevo-smtp-key

# OR Email - GMAIL
# EMAIL_HOST=smtp.gmail.com
# EMAIL_PORT=587
# EMAIL_USERNAME=your-email@gmail.com
# EMAIL_PASSWORD=your-app-password

# CORS
ALLOWED_ORIGINS=http://localhost:5173

# Frontend URL
FRONTEND_URL=http://localhost:5173
```

### 5. Run Application

```bash
# Build and run
mvn spring-boot:run

# Or build JAR and run
mvn clean package -DskipTests
java -jar target/mindcarex.jar
```

Server starts at: http://localhost:8080

### 6. Verify Setup

```bash
# Health check
curl http://localhost:8080/health

# Expected: {"status":"ok","version":"1.0.0"}
```

---

## ⚙️ Configuration

### application.yaml

**Location:** `src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: mindcarex

  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  data:
    mongodb:
      uri: ${MONGODB_URI}

  mail:
    host: ${EMAIL_HOST:smtp-relay.brevo.com}
    port: ${EMAIL_PORT:587}
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

server:
  port: ${PORT:8080}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 hours

app:
  frontend:
    url: ${FRONTEND_URL:http://localhost:5173}
```

### Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL JDBC connection | `jdbc:postgresql://host/db?sslmode=require` |
| `DATABASE_USER` | PostgreSQL username | `neondb_owner` |
| `DATABASE_PASSWORD` | PostgreSQL password | `npg_xxxxx` |
| `MONGODB_URI` | MongoDB connection string | `mongodb+srv://user:pass%40123@cluster...` |
| `JWT_SECRET` | JWT signing key (32+ chars) | `your-secret-key-here` |
| `EMAIL_HOST` | SMTP server | `smtp-relay.brevo.com` or `smtp.gmail.com` |
| `EMAIL_PORT` | SMTP port | `587` |
| `EMAIL_USERNAME` | Email account | `your-email@example.com` |
| `EMAIL_PASSWORD` | SMTP key/app password | Brevo SMTP key or Gmail app password |
| `ALLOWED_ORIGINS` | CORS allowed origins | `https://mindcarex.vercel.app` |
| `FRONTEND_URL` | Frontend base URL | `https://mindcarex.vercel.app` |

---

## 📡 API Documentation

### Base URLs

- **Local:** `http://localhost:8080`
- **Production:** `https://mindcarex-backend.onrender.com`

### Authentication

Protected endpoints require JWT token:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Endpoints Summary

| Method | Endpoint | Auth | Role | Description |
|--------|----------|------|------|-------------|
| POST | `/api/auth/register` | None | Public | Register user |
| POST | `/api/auth/login` | None | Public | Login user |
| GET | `/api/profile` | JWT | Any | Get profile |
| PUT | `/api/profile` | JWT | Any | Update profile |
| POST | `/api/appointments` | JWT | PATIENT | Book appointment |
| GET | `/api/appointments/my` | JWT | PATIENT | Get patient appointments |
| GET | `/api/doctor/appointments` | JWT | DOCTOR | Get doctor appointments |
| POST | `/api/appointments/{id}/cancel` | JWT | Both | Cancel appointment |
| POST | `/api/sessions/{appointmentId}/start` | JWT | DOCTOR | Start session |
| GET | `/api/sessions/{sessionId}` | JWT | Both | Get session details |
| POST | `/api/sessions/{sessionId}/end` | JWT | DOCTOR | End session |
| GET | `/api/sessions/{sessionId}/chat` | JWT | Both | Get chat history |
| GET | `/api/notifications/history` | JWT | Any | Get email history |
| POST | `/api/notifications/test` | JWT | Any | Send test email |
| GET | `/api/users/doctors` | JWT | Any | Get all doctors |

### Example Requests

#### Register User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "doctor@test.com",
    "password": "Test123!",
    "fullName": "Dr. John Doe",
    "role": "DOCTOR",
    "specialization": "Psychiatry",
    "licenseNumber": "LIC12345"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "uuid",
  "role": "DOCTOR",
  "email": "doctor@test.com",
  "fullName": "Dr. John Doe"
}
```

#### Test Email

```bash
curl -X POST http://localhost:8080/api/notifications/test \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'
```

---

## 🗄️ Database Schema

### PostgreSQL Tables

**users** - Authentication & base user info  
**doctors** - Doctor profiles (specialization, license, etc.)  
**patients** - Patient profiles (medical history, emergency contacts)  
**appointments** - Scheduled consultations  
**sessions** - Video session records  
**email_logs** - Email delivery tracking

### MongoDB Collections

**chat_messages** - Session chat with TTL index (24h auto-delete)

---

## 🔌 WebSocket Guide

### Connection

```javascript
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  reconnectDelay: 5000
});

client.activate();
```

### WebRTC Signaling

**Send:**
```javascript
client.publish({
  destination: `/app/signal/${sessionId}`,
  body: JSON.stringify({
    type: 'offer' | 'answer' | 'ice',
    sdp: '...',
    candidate: {...},
    from: userId  // CRITICAL for filtering
  })
});
```

**Receive:**
```javascript
client.subscribe(`/topic/signal/${sessionId}`, (msg) => {
  const signal = JSON.parse(msg.body);
  if (signal.from === userId) return; // Filter own signals
  handleSignal(signal);
});
```

### Chat Messages

**Send:**
```javascript
client.publish({
  destination: `/app/chat/${sessionId}`,
  body: JSON.stringify({
    sessionId, senderId, senderRole: 'DOCTOR', message: 'Hello'
  })
});
```

**Receive:**
```javascript
client.subscribe(`/topic/chat/${sessionId}`, (msg) => {
  const chatMsg = JSON.parse(msg.body);
  displayMessage(chatMsg);
});
```

---

## 📧 Email System

### Email Types

- **APPOINTMENT_BOOKED** - Confirmation to patient + notification to doctor
- **SESSION_REMINDER** - 10 minutes before session (both parties)
- **APPOINTMENT_CANCELLED** - Cancellation notice
- **SESSION_SUMMARY** - Post-session summary with AI insights
- **GUARDIAN_NOTIFICATION** - Summary to emergency contact

### Templates

Location: `src/main/resources/templates/email/`

- `appointment-booked-patient.html`
- `appointment-booked-doctor.html`
- `session-reminder.html`
- `appointment-cancelled.html`
- `session-summary.html`
- `guardian-session-summary.html`
- `test-email.html`

### Scheduled Reminders

**Cron:** `0 * * * * *` (every minute)

Finds appointments 10-11 minutes ahead and sends reminder emails.

### Testing Email

```bash
# Send test email
curl -X POST http://localhost:8080/api/notifications/test \
  -H "Authorization: Bearer YOUR_TOKEN"

# Check email history
curl http://localhost:8080/api/notifications/history \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 🔒 Security

### JWT Token

```json
{
  "sub": "user@example.com",
  "role": "DOCTOR",
  "iat": 1709654400,
  "exp": 1709740800
}
```

### Roles

- `ROLE_ADMIN` - System administration
- `ROLE_DOCTOR` - Start/end sessions, view doctor appointments
- `ROLE_PATIENT` - Book appointments, view patient appointments

### Password Security

- **Algorithm:** BCrypt (default strength: 10 rounds)
- **Storage:** Only hash stored in database

---

## 🚢 Deployment

### Render Deployment

1. **Push to GitHub**
   ```bash
   git add .
   git commit -m "Deploy to production"
   git push origin main
   ```

2. **Create Web Service on Render**
   - Go to [render.com](https://render.com)
   - New → Web Service
   - Connect GitHub repository
   - Settings:
     ```
     Build Command: mvn clean install -DskipTests
     Start Command: java -jar target/mindcarex.jar
     Instance Type: Free
     ```

3. **Add Environment Variables**
   ```bash
   DATABASE_URL=jdbc:postgresql://ep-xxx.aws.neon.tech/neondb?sslmode=require
   DATABASE_USER=neondb_owner
   DATABASE_PASSWORD=npg_xxxxx
   MONGODB_URI=mongodb+srv://...
   JWT_SECRET=production-secret-32-chars-minimum
   EMAIL_HOST=smtp-relay.brevo.com
   EMAIL_PORT=587
   EMAIL_USERNAME=your-email@example.com
   EMAIL_PASSWORD=your-brevo-smtp-key
   ALLOWED_ORIGINS=https://mindcarex.vercel.app
   FRONTEND_URL=https://mindcarex.vercel.app
   ```

4. **Deploy!**

**Deployment Time:** 5-7 minutes

### Keep-Alive (Prevent Sleep)

Use [cron-job](https://cron-job.org):

```
Monitor Type: HTTP(s)
URL: https://your-backend.onrender.com/api/auth/login
Interval: 5 minutes
```

---

## 🐛 Troubleshooting

### Email Not Sending

**With Brevo:**
```bash
# Verify credentials
EMAIL_HOST=smtp-relay.brevo.com  # NOT smtp.brevo.com
EMAIL_PORT=587                    # NOT 465
EMAIL_PASSWORD=your-actual-smtp-key  # NOT your Brevo account password
```

**With Gmail:**
```bash
# Use App Password (16 chars, no spaces)
EMAIL_PASSWORD=abcdabcdabcdabcd
```

### MongoDB Connection Failed

```bash
# URL-encode special characters in password
# @ becomes %40
# # becomes %23
mongodb+srv://user:pass%40word@cluster...
```

### Port 587 vs 465

```
✅ CORRECT: Port 587 with STARTTLS
❌ WRONG: Port 465 (SSL/TLS, deprecated)
```

---

## 📞 Support

- **Issues:** [GitHub Issues](https://github.com/yourusername/mindcarex-backend/issues)
- **Email:**  minorprojectcsd@gmail.com

---

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io) - Framework
- [Neon](https://neon.tech) - PostgreSQL hosting
- [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) - MongoDB hosting
- [Brevo](https://www.brevo.com) - Email service
- [Render](https://render.com) - Deployment platform

---

**Built with ❤️ by the MindCareX Team**

**⭐ Star this repo if you find it helpful!**
