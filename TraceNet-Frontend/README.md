# TraceNest — Frontend Web Application

[![React](https://img.shields.io/badge/React-19.2.0-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.8.3-blue.svg)](https://www.typescriptlang.org/)
[![TanStack Router](https://img.shields.io/badge/TanStack_Router-1.170-green.svg)](https://tanstack.com/router)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4.2.1-38bdf8.svg)](https://tailwindcss.com/)
[![Leaflet](https://img.shields.io/badge/Leaflet-1.9.4-brightgreen.svg)](https://leafletjs.com/)
[![Vite](https://img.shields.io/badge/Vite-8.0-purple.svg)](https://vitejs.dev/)
[![License: MIT](https://img.shields.io/badge/License-MIT-lightgrey.svg)](../LICENSE)

The **TraceNet Frontend** is a modern, responsive single-page and SSR-ready web application built using **React 19**, **TypeScript**, **TanStack Router / TanStack Start**, and **Tailwind CSS 4**. It provides community members and report owners with an intuitive interface to report missing cases, locate items on interactive maps, manage alerts, download printable PDF posters, and receive live notifications.

---

## Table of Contents

1. [Architecture & Tech Stack](#architecture--tech-stack)
2. [Project Structure](#project-structure)
3. [Route Map](#route-map)
4. [Prerequisites & Installation](#prerequisites--installation)
5. [Configuration](#configuration)
6. [Available Scripts](#available-scripts)
7. [Key Features & Workflows](#key-features--workflows)
   - [Authentication & Session Hydration](#1-authentication--session-hydration)
   - [Report Creation & Category Forms](#2-report-creation--category-forms)
   - [Interactive Geolocation & Leaflet Map](#3-interactive-geolocation--leaflet-map)
   - [Case Tools & PDF Poster Download](#4-case-tools--pdf-poster-download)
   - [My Reports Case Management](#5-my-reports-case-management)
   - [Real-Time In-App Notifications](#6-real-time-in-app-notifications)
   - [Public Alert Subscription with SMS OTP](#7-public-alert-subscription-with-sms-otp)
8. [Design System & UI Components](#design-system--ui-components)

---

## Architecture & Tech Stack

- **UI Framework**: React 19 with TypeScript 5.8
- **Routing & State**:
  - TanStack Router 1.170 (type-safe file-based routing)
  - TanStack Query 5.101 (React Query for server-state caching and synchronization)
  - TanStack Start & Nitro server engine
- **Styling**: Tailwind CSS 4 with CSS variable design tokens and glassmorphism accents
- **Component Primitives**: Radix UI (shadcn) accessible primitives
- **Interactive Maps**: Leaflet 1.9 & React-Leaflet with OpenStreetMap Nominatim reverse geocoding
- **Animations & Icons**: Framer Motion & Lucide React
- **Form Handling**: React Hook Form with Zod validation
- **Notifications & Feedback**: Sonner toast notifications
- **Build Tooling**: Vite 8 & Nitro

---

## Project Structure

```
TraceNet-Frontend/
├── package.json                   # Dependencies, devDependencies, and npm scripts
├── vite.config.ts                 # Vite bundler plugins (TanStack Start, React, Tailwind)
├── tsconfig.json                  # TypeScript compiler settings & path aliases
├── backend.md                     # Backend API contract specification
│
└── src/
    ├── assets/                    # Static image assets, banners, and logos
    │   └── TraceNest Banner.png
    │
    ├── components/
    │   ├── maps/                  # Geolocation & Mapping
    │   │   ├── ClientMap.tsx      # Client-side dynamic Leaflet wrapper
    │   │   └── LeafletMap.tsx     # Map renderer with custom status markers
    │   ├── reports/
    │   │   └── ReportCard.tsx     # Reusable card component for report listings
    │   ├── site/
    │   │   ├── Navbar.tsx         # Responsive navbar with desktop & mobile drawer
    │   │   └── ThemeToggle.tsx    # Light / dark theme switcher
    │   └── ui/                    # Radix UI primitives (Button, Input, Dialog, etc.)
    │
    ├── lib/                       # Utilities and context providers
    │   ├── api.ts                 # Axios HTTP client with JWT interceptor
    │   ├── auth-context.tsx       # Authentication React context provider & hooks
    │   ├── geocode.ts             # Browser GPS detection & Nominatim geocoding
    │   ├── report-utils.ts        # Status colors, category labels, timeAgo helpers
    │   └── utils.ts               # clsx and tailwind-merge helpers
    │
    ├── routes/                    # File-based routes (TanStack Router)
    │   ├── __root.tsx             # Root layout wrapping Navbar, Footer, and Toaster
    │   ├── _authenticated.tsx     # AuthGate layout protecting authenticated routes
    │   │
    │   ├── index.tsx              # Landing page (Hero, Stats, Feature showcase)
    │   ├── login.tsx              # User login form
    │   ├── register.tsx           # User registration form
    │   ├── forgot-password.tsx    # Password reset email request form
    │   │
    │   ├── reports.index.tsx      # Browse all reports with search & category filters
    │   ├── reports.$id.tsx        # Case details view with map, contact & tools
    │   ├── reports.map.tsx        # Full-page interactive map view
    │   ├── subscribe.tsx          # Public SMS/Email alert subscription with OTP
    │   │
    │   ├── _authenticated.dashboard.tsx            # User overview with personal & community stats
    │   ├── _authenticated.my-reports.tsx           # Owner case management, status toggles, deletion
    │   ├── _authenticated.reports.new.index.tsx    # Category selection for new report
    │   ├── _authenticated.reports.new.$type.tsx    # Category-specific report creation form
    │   ├── _authenticated.reports.$id.edit.tsx     # Report edit form with prefilled values
    │   ├── _authenticated.notifications.tsx        # Live in-app alert notifications center
    │   ├── _authenticated.profile.tsx              # Profile editor & Cloudinary avatar upload
    │   └── _authenticated.settings.tsx             # Password change and account settings
    │
    └── services/                  # Backend API client adapters
        ├── api.ts                 # Axios instance with auth headers & token refresh
        ├── types.ts               # TypeScript data models (Report, User, Filters)
        ├── reports.service.ts     # Reports API (CRUD, stats, my-reports, poster URL)
        ├── subscription.service.ts# OTP & alert subscription API
        ├── notifications.service.ts# In-app notifications API (fetch, mark-as-read)
        └── mock-data.ts           # Cleaned production mock stub
```

---

## Route Map

### Public Routes

| Path               | Component             | Purpose                                                            |
| ------------------ | --------------------- | ------------------------------------------------------------------ |
| `/`                | `index.tsx`           | Landing page with platform overview & search entry point           |
| `/login`           | `login.tsx`           | Account sign-in                                                    |
| `/register`        | `register.tsx`        | New user account registration                                      |
| `/forgot-password` | `forgot-password.tsx` | Password reset request                                             |
| `/reports`         | `reports.index.tsx`   | Searchable directory of missing persons, pets, and vehicles        |
| `/reports/:id`     | `reports.$id.tsx`     | Single case dossier with images, map, contact, and poster download |
| `/reports/map`     | `reports.map.tsx`     | Interactive geospatial map of all active reports                   |
| `/subscribe`       | `subscribe.tsx`       | Sign up for nearby emergency alerts with SMS OTP verification      |

### Authenticated Routes (`_authenticated` Gate)

| Path                 | Component                              | Purpose                                                             |
| -------------------- | -------------------------------------- | ------------------------------------------------------------------- |
| `/dashboard`         | `_authenticated.dashboard.tsx`         | User statistics overview, quick actions, recent reports             |
| `/my-reports`        | `_authenticated.my-reports.tsx`        | Manage authored cases: update status, edit, download poster, delete |
| `/reports/new`       | `_authenticated.reports.new.index.tsx` | Select category (`person`, `pet`, `lost-vehicle`, `stolen-vehicle`) |
| `/reports/new/:type` | `_authenticated.reports.new.$type.tsx` | Multi-image upload, GPS location picker, type-specific attributes   |
| `/reports/:id/edit`  | `_authenticated.reports.$id.edit.tsx`  | Edit details and adjust map pin of an existing report               |
| `/notifications`     | `_authenticated.notifications.tsx`     | Live notification feed with mark-read and report navigation         |
| `/profile`           | `_authenticated.profile.tsx`           | Edit personal details and upload avatar                             |
| `/settings`          | `_authenticated.settings.tsx`          | Change password and manage notification preferences                 |

---

## Prerequisites & Installation

1. **Node.js**: `v20.x` or `v24.x` (LTS recommended)
2. **Package Manager**: `npm` (or `bun`)

### Install Dependencies

```bash
cd TraceNet-Frontend
npm install
```

---

## Configuration

The API client connects to the Spring Boot backend via [`src/services/api.ts`](file:///d:/koyan/Documents/OneDrive/Desktop/TraceNest/TraceNet-Frontend/src/services/api.ts).

By default, the base URL is:

```typescript
const BASE_URL = "http://localhost:8080/api/v1";
```

When deploying to production, point this to your backend domain (e.g., `https://api.tracenest.app/api/v1`).

---

## Available Scripts

In `TraceNet-Frontend/`:

- **`npm run dev`**: Starts the local development server with Vite hot-reloading at `http://localhost:5173`.
- **`npm run build`**: Builds the SSR-ready, client-optimized production bundle using Vite and Nitro into `.output/`.
- **`npm run preview`**: Starts a local preview of the production build.
- **`npm run lint`**: Runs ESLint across all TypeScript and React files.

---

## Key Features & Workflows

### 1. Authentication & Session Hydration

- Managed via [`src/lib/auth-context.tsx`](file:///d:/koyan/Documents/OneDrive/Desktop/TraceNest/TraceNet-Frontend/src/lib/auth-context.tsx).
- On application boot, checks local storage for tokens and validates credentials via `GET /api/v1/auth/me`.
- Axios interceptor in [`src/services/api.ts`](file:///d:/koyan/Documents/OneDrive/Desktop/TraceNest/TraceNet-Frontend/src/services/api.ts) automatically appends `Authorization: Bearer <token>` to requests and attempts silent token refresh on `401 TOKEN_EXPIRED`.

### 2. Report Creation & Category Forms

- Accessed at `/reports/new/:type` for 4 categories:
  - **Person**: Age, Gender, Clothing details, Last seen date/time.
  - **Pet**: Species, Breed, Color, Last seen date/time.
  - **Lost Vehicle**: Vehicle type, Registration number, Make, Model, Year.
  - **Stolen Vehicle**: Same as Lost Vehicle plus FIR Number and Police Station.
- Multi-image file picker uploading up to 5 photos to Cloudinary.

### 3. Interactive Geolocation & Leaflet Map

- Powered by [`ClientMap.tsx`](file:///d:/koyan/Documents/OneDrive/Desktop/TraceNest/TraceNet-Frontend/src/components/maps/ClientMap.tsx) and [`geocode.ts`](file:///d:/koyan/Documents/OneDrive/Desktop/TraceNest/TraceNet-Frontend/src/lib/geocode.ts).
- One-click **Use GPS** button auto-locates current coordinates using HTML5 Geolocation.
- Reverse geocoding via OpenStreetMap Nominatim automatically resolves addresses, cities, postcodes, and states.
- Interactive map allows clicking anywhere to drop or move a pin.
- Custom color-coded map markers:
  - 🔵 **Blue**: Missing cases
  - 🟢 **Green**: Found cases
  - 🟡 **Gold**: Resolved cases

### 4. Case Tools & PDF Poster Download

- Every report detail page (`/reports/:id`) contains a **Case Tools** section.
- Includes a direct link to **Download Printable Poster (PDF)** pointing to `GET /api/v1/reports/:id/poster`.
- Generates an A4 missing poster in the browser for printing and physical distribution.

### 5. My Reports Case Management

- Located at `/my-reports` for authenticated users.
- Live tabs for filtering by status: **All**, **Missing**, **Found**, **Resolved**.
- Immediate status toggles without leaving the page.
- Direct links to **View**, **Edit**, **Download Poster**, and **Delete Report** (with confirmation dialog).

### 6. Real-Time In-App Notifications

- Located at `/notifications`.
- Displays alerts when new cases are reported within 20KM of the user or when authored reports change status.
- Unread status indicator dots and **Mark all read** action.
- Clicking any notification navigates directly to the corresponding report.

### 7. Public Alert Subscription with SMS OTP

- Located at `/subscribe`.
- Allows community members to subscribe to local emergency alerts.
- Integrates phone number verification with 6-digit OTP sent via Twilio SMS.

---

## Design System & UI Components

- **Color Palette**:
  - `brand-blue`: `#2563eb` (Missing cases & primary actions)
  - `brand-green`: `#16a34a` (Found cases & confirmations)
  - `brand-gold`: `#d97706` (Resolved cases & rewards)
- **Dark & Light Mode**: Seamless theme switching via `ThemeToggle.tsx` and CSS root variables.
- **Card Design**: Soft shadows (`--shadow-soft`), rounded corners (`rounded-2xl`), and subtle micro-animations using Framer Motion.
