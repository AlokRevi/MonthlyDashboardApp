# Monthly Dashboard

Monthly Dashboard is a Spring Boot + Angular app for tracking recurring monthly tasks, due-today items, overdue items, and completed task occurrences.

## Active App Paths

- Active frontend app: `frontend-angular/frontend-angular`
- Backend API app: `backend-java`
- Archived frontend prototypes: `archive/frontend-prototypes`

The folders under `archive/frontend-prototypes` are kept only for reference. New frontend work should happen in `frontend-angular/frontend-angular`.

## Run The Backend

From the repo root:

```powershell
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

## Run The Frontend

From the repo root:

```powershell
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

```powershell
$env:APP_CORS_ALLOWED_ORIGINS="https://your-domain.com"
```

## Verify

Backend:

```powershell
cd backend-java
mvn test
```

Frontend:

```powershell
cd frontend-angular/frontend-angular
npm run build
```
