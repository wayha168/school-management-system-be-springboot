# Assignment microservice setup

Homework assignments and **in-app class video meetings** live in **assignment-service** (port **8082**).  
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

## Classroom video meetings

1. **Classes** → open a class  
2. **Start video call** — title + optional schedule + checkbox **Allow recording & store video**  
3. The service generates a **room code** and join path: `/admin/classroom/call/{roomCode}`  
4. Use **Copy link** / **Join** — camera/mic in the browser (WebRTC). No Meet/Zoom URL.  
5. If recording is enabled, host clicks **Start recording** → **Stop & save** to store the video on the assignment-service disk (`app.recordings.dir`)  
6. Dashboard lists live meetings with Join  

### Assignment file / image submit

On class detail, students can submit **text and/or a file/image** (`enctype=multipart`).  
Files are stored under `app.submissions.dir` (Docker: `/data/submissions`).  
Teachers open **Submissions** to view answers and download attachments.

Allowed: images (jpg, png, gif, webp, …) and common docs (pdf, doc/docx, xls/xlsx, ppt/pptx, txt, zip, csv).

Recordings are served at `/admin/classroom/meetings/{id}/recording`.

## “Classroom service unavailable”

1. Open http://localhost:8082/health — must return `{"status":"UP",...}`  
2. Confirm DB `school-assignment` exists  
3. If using Docker: `docker compose logs assignment-service`  
4. If old meetings lack `room_code`, end them and create a new video call
