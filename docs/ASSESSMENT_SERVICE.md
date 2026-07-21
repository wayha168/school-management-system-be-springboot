# Assessment microservice setup

Scores / GPA live in **assessment-service** (port **8081**).  
The main app (port **8080**) is a BFF — it cannot save scores unless assessment is running.

## Real logins (main app http://localhost:8080/login)

| Email | Password |
|-------|----------|
| `admin@school.com` | `admin123` |
| `superadmin@school.com` | `superadmin123` |

Postgres password: `0508`

## Databases (host Postgres)

Create both if missing:

```sql
CREATE DATABASE "school-management";
CREATE DATABASE "school-assessment";
```

## Option A — Docker (both microservices)

`docker compose` now uses `docker-compose.yml` (apps).  
Infra-only Postgres is `compose.infra.yaml` (do **not** use it if Windows Postgres already owns 5432).

```powershell
# Make sure Docker Desktop is running
docker compose up --build
```

- Main UI: http://localhost:8080  
- Assessment API: http://localhost:8081/health  

Inside Docker, school-management calls `http://assessment-service:8081` (set via `APP_ASSESSMENT_BASE_URL`).

## Option B — Main app in IDE + assessment in Docker

Scores fail if only the main project is running.

```powershell
docker compose up --build assessment-service
```

Then run `SchoolManagementApplication` in the IDE.  
Default `app.assessment.base-url` is `http://localhost:8081`.

## Option C — Both via Maven

```powershell
mvn -f assessment-service/pom.xml spring-boot:run
mvn spring-boot:run
```

## “Unable to load / input scores”

Usually means assessment is down or unreachable:

1. Open http://localhost:8081/health — must return `{"status":"UP",...}`
2. If running only the main app: start assessment (Option B or C)
3. If using Docker: `docker compose logs assessment-service`
4. Confirm DB `school-assessment` exists on host Postgres
