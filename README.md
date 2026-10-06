# SkillSwap 🎓⚡
> **Peer-to-Peer Campus Skill Exchange Platform**  
> Teach what you know. Learn what you need. Zero fiat currency — powered by a transparent 1:1 Time-Credit protocol.

---

## 🌟 Overview

**SkillSwap** is a full-stack campus collaboration platform engineered to democratize peer learning among university students. Instead of paying monetary tutoring fees, students trade time:
- **1 Hour Teaching = 1 Time Credit (TC) Earned**
- **1 Hour Learning = 1 Time Credit (TC) Spent**
- Every registered student receives **5.0 Free Time Credits** as a signup bonus to kickstart their learning journey.

The platform is backed by a production-ready **Spring Boot 4** backend (Java 21) with an immutable time-credit ledger, deterministic peer matching algorithm, WebSocket-based real-time chat, and a responsive **React 19** frontend powered by Vite and Tailwind CSS.

---

## 🚀 Key Features

### 1. ⏱️ Time-Credit Wallet & Escrow Ledger
- **Immutable Transaction History**: Complete audit trail for credits earned, credits spent, review bonuses, and admin grants.
- **Session Escrow**: When a student requests a session, the credit is locked in escrow and released automatically only after mutual completion confirmation.

### 2. 🧠 Deterministic Peer Matching Engine
- Multi-factor scoring model that matches students based on:
  - **Skill Reciprocity**: Matching teachers with learners who offer skills the teacher wants.
  - **Proficiency Compatibility**: Beginner / Intermediate / Advanced alignments.
  - **Schedule & Availability Overlap**: Direct alignment of free slots.

### 3. 💬 Real-Time Chat & Live Presence (STOMP / WebSocket)
- Instant messaging between matched peers using Spring WebSocket and STOMP messaging.
- Presence tracking indicating when peers are active or in sessions.

### 4. 🏆 Campus Gamification & Community
- **XP Progression & Ranks**: Earn XP for mentoring peers, completing sessions, and receiving 5-star ratings.
- **Skill Badges & Achievements**: Recognitions like *Campus Mentor*, *Speedy Learner*, and *Subject Specialist*.
- **Learning Groups & Challenges**: Campus-wide hackathon prep or study circles.

### 5. 🛡️ Security & Administration
- **JWT Authentication**: Stateless, tamper-proof session tokens with role-based access control (`STUDENT`, `MODERATOR`, `ADMIN`).
- **Admin & Moderation Console**: User management, session audit logs, transaction tracking, and dispute resolution.
- **Realistic Campus Data Seeder**: Instant bootstrapping with 14 categories, 51 skills, and 25 demo students.

---

## 🏗️ Architecture & Tech Stack

```
SkillSwap/
├── backend/                   # Spring Boot 4 REST API & WebSocket Server
│   ├── src/main/java/com/skillswap/
│   │   ├── admin/             # Analytics, reports & moderation
│   │   ├── auth/              # JWT auth controllers & services
│   │   ├── common/            # Unified ApiResponse envelope & error handling
│   │   ├── community/         # Groups, challenges, XP & badges
│   │   ├── connection/        # Student peer connections & requests
│   │   ├── match/             # Deterministic match calculator & queries
│   │   ├── realtime/          # STOMP broker, presence & chat
│   │   ├── session/           # Session scheduling, lifecycle & escrow
│   │   ├── skill/             # Skill catalogue, proficiency & user skills
│   │   ├── user/              # User profiles, cards & avatars
│   │   └── wallet/            # Time wallet ledger & credit transactions
│   └── pom.xml                # Maven build configuration
│
└── frontend/                  # Vite + React 19 Client
    ├── src/
    │   ├── App.jsx            # Interactive SkillSwap campus portal
    │   ├── index.css          # Tailwind CSS styling & glassmorphic tokens
    │   └── main.jsx           # Application entry point
    ├── vite.config.js         # Vite configuration with Tailwind plugin
    └── package.json           # Dependencies (React 19, Lucide, StompJS, Recharts)
```

| Layer | Technologies |
|---|---|
| **Backend** | Spring Boot 4.1.1, Java 21, Spring Security, Spring Data JPA, Hibernate, WebSocket / STOMP |
| **Database** | MySQL (Production/Dev), In-Memory H2 (Hermetic Unit & Integration Tests) |
| **Frontend** | React 19, Vite, Tailwind CSS v4, Lucide React, Axios, Recharts, StompJS |
| **Authentication** | JWT (JSON Web Tokens), BCrypt Password Hashing, Role-Based Access Control |

---

## ⚡ Quickstart & Setup

### Prerequisites
- **Java 21** or later
- **Node.js 18+** and **npm**
- **Git**

### 1. Backend Setup
```bash
cd backend

# Run the full test suite (20 unit and integration tests against H2)
./mvnw test

# Start the Spring Boot backend server on port 8080
./mvnw spring-boot:run
```

*By default, the backend seeds demo accounts on startup. You can log in as `admin@skillswap.dev` or any demo student profile.*

### 2. Frontend Setup
```bash
cd ../frontend

# Install dependencies
npm install

# Start the development server
npm run dev

# Build production bundle
npm run build
```

---

## 📡 API Endpoints Summary

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register new student profile (+5 TC bonus) |
| `POST` | `/api/auth/login` | Authenticate and obtain JWT token |
| `GET` | `/api/skills` | List searchable skill catalogue |
| `GET` | `/api/matches` | Get deterministic peer matches for current student |
| `POST` | `/api/connections` | Send connection swap request |
| `POST` | `/api/sessions/book` | Book 1-hour session (locks 1 TC into escrow) |
| `POST` | `/api/sessions/{id}/complete` | Confirm session completion & release credits |
| `GET` | `/api/wallet` | Fetch current wallet balance & transaction ledger |
| `GET` | `/api/admin/metrics` | Campus metrics & moderation overview (Admin only) |

---

## 📄 License
This project is licensed under the MIT License.
