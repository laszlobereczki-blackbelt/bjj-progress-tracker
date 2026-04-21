# BJJ Progression Tracker — Plan

A single-user Vaadin / Spring Boot application for tracking personal Brazilian Jiu-Jitsu progression: training sessions, competitions, and belt promotions. The existing `examplefeature/Task` code is placeholder scaffolding and will be deleted once real features start landing.

## V1 scope

Feature packages replacing `examplefeature`:

### `hu.blackbelt.auth`
Spring Security with a single user configured via `application.properties` (bcrypt-hashed password). Simple login form. No `User` entity — password change from the UI is not needed for v1.

### `hu.blackbelt.training`
`TrainingSession` entity:
- `date` (LocalDate)
- `durationMinutes` (int)
- `type` (enum: `GI` / `NOGI` / `GRAPPLING`)
- `location`
- `notes`

### `hu.blackbelt.competition`
`Competition` entity:
- `date`
- `name`
- `location`
- `division` (belt)
- `weightClass`
- `result` (enum: `GOLD` / `SILVER` / `BRONZE` / `PARTICIPATED`)
- `matchesWon`
- `matchesLost`
- `notes`

### `hu.blackbelt.belt`
`BeltPromotion` entity:
- `belt` (enum: `WHITE` … `BLACK`)
- `stripes` (0–4)
- `dateAwarded`
- `awardedBy`
- `academy`
- `notes`

Each stripe is its own promotion row — current belt/stripes is derived as the latest row. No separate mutable "current belt" field.

### `hu.blackbelt.dashboard`
Stats-only view at root route. Aggregations:
- Total mat time (hours)
- Split by gi / nogi / grappling (hours + %)
- Sessions this week / month / year
- Current belt + stripes, time at current belt
- Competitions: total, medal counts, W/L record

## Explicitly out of scope for v1

Do not add unless explicitly requested:
- Technique / position log per session
- Training partners
- Weekly / monthly training goals
- Multi-user, password change UI, user registration

## Build order

1. Delete the `examplefeature` package entirely.
2. Add `spring-boot-starter-security` to `pom.xml`, configure single-user auth.
3. Implement feature CRUDs in order: training sessions → belt promotions → competitions.
4. Implement the dashboard last — it's pure aggregation over the other three.
