# Monthly Dashboard

Monthly Dashboard is a Spring Boot and Angular app for managing recurring responsibilities as generated task occurrences. The monthly grid gives a read-only overview, while the Today Checklist is the main action surface for overdue and due-today work.

## What It Does

- Creates categories and recurring tasks.
- Generates monthly occurrences dynamically from recurrence rules.
- Tracks completion history by `occurrenceDate`.
- Shows overdue, due-today, upcoming, and completed occurrences.
- Groups the Today Checklist by category requirement, with completed items undoable from the checkmark.
- Exports current system data as JSON for backup.
- Supports a focused mobile view for checklist and task entry.

## Current V2 Features

- Backend-controlled current date provider for testable date logic.
- JSON export endpoint at `GET /api/v1/export`.
- Recurrence edge-case coverage for leap years, fallbacks, boundaries, and weekday rules.
- Dashboard UI split into focused Angular components with page state handled by a state service.
- Softer monthly grid colors with progressive past-day styling.
- Category metadata:
  - `requires`: `FOCUS`, `MOVEMENT`, `OUTDOOR`
  - structured category profile defaults: `energy`, `enjoyment`, `pressure`, `effort`
- Category create/edit dialog with access from the Add Task flow, monthly occurrence section, and right-side Categories panel.
- Backend-generated recurrence summaries for category/task displays.
- Today Checklist grouped by category `requires`.
- Interval recurrence supports `DAYS`, `WEEKS`, and `MONTHS`.
- Safe scoped task editing for future changes.
- Optional task-level profile overrides for `energy`, `enjoyment`, `pressure`, and `effort`.

## Recurrence Support

Current recurrence types:

- `FIXED_DATE`: one or more days of month, with optional fallback to the last valid day.
- `INTERVAL`: every N days, weeks, or months.
- `WEEKDAY`: first, second, third, fourth, or last weekday of a month.

Monthly interval behavior preserves the original start-date day when possible. If the target month does not contain that day, it falls back to the last valid day of that month.

## Scoped Editing

Implemented edit scopes:

- `THIS_AND_FOLLOWING`
- `ALL_FUTURE`

Both scopes use a safe split model:

- the existing task ends before the selected occurrence
- a new successor task starts at the selected occurrence
- existing completion history stays attached to the original task

Not implemented:

- true `THIS_OCCURRENCE`
- per-occurrence overrides for task edits
- rewriting historical recurrence meaning

## Category Profiles

Category-level profile fields are the defaults for tasks in that category:

- `requires`
- `energy`
- `enjoyment`
- `pressure`
- `effort`

The category dialog supports both create and edit mode. Categories can be edited from:

- the monthly occurrence section
- the right-side Categories panel

Task-level profile fields in the Edit Task modal are optional overrides. A `null` task override means "use category default" for that profile field.

Profile value groups:

- Energy: `DEATHLY_DRAINING`, `TIRING`, `ACTIVATING`, `ENERGIZING`
- Enjoyment: `BORING`, `OKAY`, `FUN`, `BLISSFUL`
- Pressure: `NO_PRESSURE`, `MILD_FUTURE_STRESS`, `URGENT_AND_IMPORTANT`, `AMORPHOUS_DREAD`
- Effort: `EASY`, `MEDIUM`, `HARD`, `VERY_HARD`

## Checklist Actions

The Today Checklist is the primary completion surface:

- incomplete overdue and due-today items can be marked complete
- completed-today items show a checkmark
- clicking the completed checkmark undoes that completion

## Mobile Behavior

Phone-sized screens show the action-focused flow:

- Today Checklist
- Add Task
- Create Category dialog entry point

The monthly grid, timeline, and categories management panel remain available on larger screens.

## Project Paths

- Backend API: `backend-java`
- Frontend app: `frontend-angular/frontend-angular`
- Archived prototypes: `archive/frontend-prototypes`

New frontend work should happen in `frontend-angular/frontend-angular`.

## Run The Backend

```bash
cd backend-java
mvn spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

Default local SQLite database:

```text
backend-java/monthly-dashboard.db
```

## Run The Frontend

```bash
cd frontend-angular/frontend-angular
npm install
npm start
```

Frontend URL:

```text
http://localhost:4200
```

## Configuration

Angular API URLs:

```text
frontend-angular/frontend-angular/src/environments/environment.ts
frontend-angular/frontend-angular/src/environments/environment.prod.ts
```

Backend CORS configuration:

```text
backend-java/src/main/resources/application.properties
```

Deployment example:

```powershell
$env:APP_CORS_ALLOWED_ORIGINS="https://your-domain.com"
```

## Verify

Backend:

```bash
cd backend-java
mvn test
```

Frontend:

```bash
cd frontend-angular/frontend-angular
npm run build
```

## Current Limitations

- The app enforces a maximum of 15 active tasks.
- Import is not implemented yet.
- True single-occurrence editing is deferred.
- Task-level profile overrides exist, but they are simple optional fields only; they do not change recurrence or completion behavior.
- The monthly grid is intentionally read-only; checklist actions are the primary completion flow.
