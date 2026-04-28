# Monthly Dashboard

A structured monthly task system that converts recurrence rules into trackable occurrences, reducing cognitive load.

Monthly Dashboard is a Spring Boot + Angular app for tracking recurring monthly tasks, due-today items, overdue items, and completed task occurrences.

## System Flow

```text
User defines rules -> system generates occurrences -> user completes -> system tracks history
```

## Active App Paths

- Active frontend app: `frontend-angular/frontend-angular`
- Backend API app: `backend-java`
- Archived frontend prototypes: `archive/frontend-prototypes`

The folders under `archive/frontend-prototypes` are kept only for reference. New frontend work should happen in `frontend-angular/frontend-angular`.

## Run the Backend

From the repo root:

```bash
cd backend-java
mvn spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

The default local SQLite database is:

```text
backend-java/monthly-dashboard.db
```

## Run the Frontend

From the repo root:

```bash
cd frontend-angular/frontend-angular
npm install
npm start
```

The frontend runs on:

```text
http://localhost:4200
```

## Configuration

The Angular development API URL is configured in:

```text
frontend-angular/frontend-angular/src/environments/environment.ts
```

The Angular production API URL is configured in:

```text
frontend-angular/frontend-angular/src/environments/environment.prod.ts
```

Backend CORS origins are configured in:

```text
backend-java/src/main/resources/application.properties
```

For deployment, set:

```bash
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
