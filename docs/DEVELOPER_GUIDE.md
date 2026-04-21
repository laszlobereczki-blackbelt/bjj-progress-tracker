# BJJ Progression Tracker — Developer Guide

## Overview

BJJ Progression Tracker is a single-user web application for tracking Brazilian Jiu-Jitsu training sessions, belt promotions, competition results, and personal fitness metrics. It is built with **Java 21**, **Spring Boot 4**, **Vaadin 25**, and **Spring Data JPA** backed by an H2 in-memory database.

---

## Quick Start

### Prerequisites

- Java 21+
- Maven (or use the included `./mvnw` wrapper)

### Run in Development Mode

```bash
./mvnw
```

The app starts at [http://localhost:8080](http://localhost:8080).

Default credentials: `admin` / `bjj2024` (configured via `app.auth.password` in `application.properties`).

### Run Tests

```bash
./mvnw test
./mvnw test -Dtest=TaskServiceTest              # single class
./mvnw test -Dtest=TaskServiceTest#method_name  # single method
```

### Build for Production

```bash
./mvnw -Pproduction package
docker build -t bjj-tracker:latest .
```

---

## Architecture

The project uses a **feature-based package structure**. Each feature is a self-contained vertical slice that owns its entity, repository, service, UI view, and tests. There are no horizontal `controller/service/repository` layers.

```
hu.blackbelt/
├── Application.java          — Spring Boot entry point + Vaadin @Theme
├── auth/                     — Login view and Spring Security configuration
├── base/                     — Shared infrastructure (layout, dummy data)
│   └── ui/                   — MainLayout, ViewTitle (reused by all views)
├── dashboard/                — Root view ("/") with aggregated statistics
├── training/                 — Training session CRUD + analytics
├── belt/                     — Belt promotion CRUD
├── competition/              — Competition CRUD + win/loss analytics
├── profile/                  — User biometric profile + calculated stats
└── help/                     — In-app help/documentation view
```

---

## Modules

### `auth`

| File | Role |
|---|---|
| `SecurityConfig.java` | Spring Security + Vaadin security integration. In-memory user `admin`, password from `app.auth.password`. BCrypt encoding. |
| `LoginView.java` | `@Route("login")`, `@AnonymousAllowed`. Vaadin `LoginForm` with centered layout. |

Password is read at startup from `application.properties`:

```java
@Value("${app.auth.password}")
private String rawPassword;
```

To change the password, update `app.auth.password` and restart. For production, move this to an environment variable or secrets manager.

---

### `base`

| File | Role |
|---|---|
| `MainLayout.java` | `AppLayout` wrapping all authenticated views. Drawer built from `@Menu` annotations. Footer: load demo data / sign out. |
| `ViewTitle.java` | Reusable page heading component used by each view. |
| `DummyDataService.java` | Loads `src/main/resources/dummy-data.json` into the database. Safe to call multiple times — it clears existing data first. |

---

### `dashboard`

`DashboardView` (`@Route("")`) is the home page. It is read-only and calls the three service classes at construction time to build four stat blocks:

- **Mat Time** — total hours + breakdown by `TrainingType` (Gi / No-Gi / Grappling)
- **Sessions** — count for this week / month / year
- **Belt** — current rank, stripe count, days at current belt
- **Competitions** — total, medals, match record

All formatting helpers (`formatHours`, `pct`, `formatDays`) are private methods in the view class.

---

### `training`

Tracks individual training sessions.

| File | Role |
|---|---|
| `TrainingSession.java` | JPA entity. Fields: `date`, `durationMinutes` (≥ 1), `type` (`TrainingType` enum), optional `location`, optional `notes`. |
| `TrainingType.java` | `GI`, `NOGI`, `GRAPPLING` |
| `TrainingSessionRepository.java` | Custom `@Query` methods: `sumDurationMinutes()`, `sumDurationMinutesByType()`, `countByDateBetween()`. |
| `TrainingSessionService.java` | Paginated list, save, delete. Aggregates: `getTotalMinutes()`, `getMinutesByType()`, `countBetween()`. |
| `TrainingSessionListView.java` | `@Route("training")`, menu order 1. Grid with lazy loading via `VaadinSpringDataHelpers`. Dialog-based add/edit form. |

---

### `belt`

Tracks belt and stripe promotions.

| File | Role |
|---|---|
| `BeltPromotion.java` | JPA entity. Fields: `belt` (`Belt` enum), `stripes` (0–4), `dateAwarded`, optional `awardedBy`, `academy`, `notes`. |
| `Belt.java` | `WHITE`, `BLUE`, `PURPLE`, `BROWN`, `BLACK` |
| `BeltPromotionRepository.java` | `findAllByOrderByDateAwardedDesc()`, `findTopByOrderByDateAwardedDesc()` |
| `BeltPromotionService.java` | Paginated list, save, delete, `getLatestPromotion()` (used by dashboard and profile). |
| `BeltPromotionListView.java` | `@Route("belt-promotions")`, menu order 2. Grid + dialog form. |

---

### `competition`

Tracks competition participation and results.

| File | Role |
|---|---|
| `Competition.java` | JPA entity. Fields: `date`, `name`, optional `location`, `division` (`Belt`), `weightClass`, `result` (`CompetitionResult`), `matchesWon`, `matchesLost`, optional `notes`. |
| `CompetitionResult.java` | `GOLD`, `SILVER`, `BRONZE`, `PARTICIPATED` |
| `CompetitionRepository.java` | `@Query` aggregates: `sumMatchesWon()`, `sumMatchesLost()`. |
| `CompetitionService.java` | Paginated list, save, delete. Aggregates: `countTotal()`, `countByResult()`, `getTotalMatchesWon()`, `getTotalMatchesLost()`. |
| `CompetitionListView.java` | `@Route("competitions")`, menu order 3. Grid + dialog form. |

---

### `profile`

Stores a single user profile record with biometric data. The view is read-only for computed stats and editable for raw profile fields.

| File | Role |
|---|---|
| `UserProfile.java` | JPA entity. Fields: `name`, `dateOfBirth`, `weightKg`, `heightCm`, `gender` (`Gender`), `lifestyle` (`Lifestyle`), `trainingHoursPerDay`. |
| `Gender.java` | `MALE`, `FEMALE` |
| `Lifestyle.java` | Enum with activity multiplier: `SEDENTARY` (1.2) → `EXTRA_ACTIVE` (1.9) |
| `UserProfileRepository.java` | `findFirstByOrderByIdAsc()` — always returns the single profile. |
| `UserProfileService.java` | BMR (Mifflin-St Jeor), BMI + category, TDEE (lifestyle × BMR + BJJ MET 8.0), protein target (2.0 g/kg), water target (35 ml/kg + 500 ml/training hour), per-session calorie burn. |
| `ProfileView.java` | `@Route("profile")`, menu order 4. Three tabs: **Profile** (edit form), **Stats** (computed metrics with tooltips), **Miscellaneous** (training summary, calorie estimates, competition record, belt journey). |

---

### `help`

| File | Role |
|---|---|
| `HelpView.java` | `@Route("help")`, menu order 5. In-app user manual with collapsible sections for each feature. |

---

## Navigation

All authenticated views declare `@Route`, `@Menu`, and `@PermitAll` (or `@RolesAllowed`). `MainLayout` reads the `@Menu` annotations at startup and builds the side drawer automatically. Menu items are sorted by their `order` attribute.

| Route | View | Menu Order |
|---|---|---|
| `/` | DashboardView | 0 |
| `/training` | TrainingSessionListView | 1 |
| `/belt-promotions` | BeltPromotionListView | 2 |
| `/competitions` | CompetitionListView | 3 |
| `/profile` | ProfileView | 4 |
| `/help` | HelpView | 5 |
| `/login` | LoginView | — |

---

## Database

H2 in-memory, schema managed by Hibernate (`ddl-auto=update`). Data is lost on restart unless the demo loader is called, or you persist the H2 file by changing the JDBC URL.

All entities use `@GeneratedValue(strategy = GenerationType.SEQUENCE)`. Equality is based on ID.

To switch to a persistent database, replace the H2 dependency with your driver of choice and update `spring.datasource.*` in `application.properties`.

---

## Demo Data

`DummyDataService.loadDemoData()` reads `src/main/resources/dummy-data.json` and populates:

- 17 training sessions (Gi / No-Gi / Grappling, April 2025 – April 2026)
- 1 belt promotion (Blue, 2 stripes, June 2025)
- 2 competitions (Spring Open — Silver, Autumn Championship — Gold)

The "Load demo data" button in the sidebar footer calls this method. Calling it again resets the data.

---

## Frontend & Theming

Vaadin renders server-side Java components to web components in the browser.

- **Theme**: `default` Lumo-based theme at `src/main/frontend/themes/default/`
- **Custom CSS**: `src/main/resources/META-INF/resources/styles.css` (app logo, sidebar footer, etc.)
- **ViewTitle CSS**: `src/main/resources/META-INF/resources/view-title.css`
- **Icons**: `src/main/resources/META-INF/resources/icons/clipboard-check.svg`

---

## Adding a New Feature

1. Create a package `hu.blackbelt.<featurename>`.
2. Add the JPA entity, repository interface, and service class following the patterns in `training` or `belt`.
3. Create a view class extending a Vaadin layout, annotated with `@Route`, `@PageTitle`, `@Menu`, and `@PermitAll`.
4. Inject `MainLayout.class` as the layout in the `@Route` annotation if needed (it is applied automatically via `RouterLayout` detection).
5. Write an integration test using `@SpringBootTest`.

Use `TrainingSessionListView` as the reference for grids with lazy loading and dialog-based forms.

---

## Security Notes

- Authentication is handled by Spring Security with a single in-memory user.
- All views (except `/login`) require authentication.
- The `app.auth.password` property holds the plaintext password; `SecurityConfig` hashes it with BCrypt at startup.
- This setup is intentionally simple for a single-user personal app. For multi-user scenarios, replace with a `UserDetailsService` backed by a database.

---

## Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| Spring Boot | 4.0.5 | Application framework |
| Vaadin | 25.1.1 | Server-side UI framework |
| Spring Data JPA | via Boot | ORM + repositories |
| H2 | via Boot | In-memory database |
| Spring Security | via Boot | Authentication |
