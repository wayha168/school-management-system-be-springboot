# Assignment microservice setup

Homework assignments and class Meet/Zoom meetings live in **assignment-service** (port **8082**).  
The main app (port **8080**) is a BFF — classroom features need this service running.

## Real logins (main app http://localhost:8080/login)

| Email | Password |
|-------|----------|
| `admin@school.com` | `admin123` |
| `superadmin@school.com` | `superadmin123` |

Postgres password: `0508`

## Databases (host Postgres)

Create if missing:

```sql
CREATE DATABASE "school-assignment";
```

## Option A — Docker (all microservices)

```powershell
docker compose up --build
```

- Main UI: http://localhost:8080  
- Assessment: http://localhost:8081/health  
- Assignment: http://localhost:8082/health  

Inside Docker, school-management calls:

- `APP_ASSESSMENT_BASE_URL=http://assessment-service:8081`
- `APP_ASSIGNMENT_BASE_URL=http://assignment-service:8082`

## Option B — Main app in IDE + services in Docker

```powershell
docker compose up --build assessment-service assignment-service
```

Then run `SchoolManagementApplication` in the IDE.  
Defaults in `application.yml`:

- `app.assignment.base-url: http://localhost:8082`

## Option C — Maven only

```powershell
mvn -f assignment-service/pom.xml spring-boot:run
mvn spring-boot:run
```

## Classroom UI

1. **Classes** page → card grid → click a class  
2. On class detail: **Start meeting** (paste Meet/Zoom URL) and **New assignment**  
3. Dashboard shows active meetings + open assignments for teachers/principals/admins  

## “Classroom service unavailable”

1. Open http://localhost:8082/health — must return `{"status":"UP",...}`  
2. Confirm DB `school-assignment` exists  
3. If using Docker: `docker compose logs assignment-service`
