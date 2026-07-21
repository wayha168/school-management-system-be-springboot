# Assessment Service

Microservice for exam scores and GPA calculation.

## Run locally

1. Create Postgres database:

```sql
CREATE DATABASE "school-assessment";
```

2. Start the service (port **8081**):

```bash
mvn -f assessment-service/pom.xml spring-boot:run
```

3. Start the main School-Management app (port **8080**). It proxies score/GPA calls here.

## Config

| Property | Default |
|----------|---------|
| `server.port` | `8081` |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/school-assessment` |
| `app.jwt.secret` | same as main app |
| `app.internal-key` | `assessment-internal-key-change-me` |

## Auth

- Browser/BFF calls: `X-Internal-Key` + `X-User-Uuid`, `X-User-Role`, `X-Authorities`, optional `X-School-Uuid`
- API clients: `Authorization: Bearer <JWT>` (same secret; JWT should include `userUuid`, `role`, `authorities`)
- Internal GPA grants: `PUT /internal/v1/gpa-access/{studentUuid}` with `X-Internal-Key` only

## Student gate

Students cannot read scores or GPA until a `gpa_access_grants` row is approved (synced when principal/admin approves a `GPA_VIEW` request in the main app).
