---
name: project-check
description: >-
  Checks the School-Management Spring Boot project for entity/schema alignment,
  Postgres config, package layout, and role/school/class relationships. Use when
  checking this repo, verifying entities, schema.sql, or application.yml.
disable-model-invocation: true
---

# School-Management Project Check

Follow personal skill `cheap-project-check` token rules. Add these repo checks.

## Expected package layout

```text
com.project.school_management
├── config/
├── controller/
│   ├── api/          # REST CRUD (@RestController)
│   └── view/         # login/dashboard (@Controller)
├── dto/
├── entities/
├── enums/
├── exception/
├── repository/
├── security/
└── service/
```

Resources: `templates/` (Thymeleaf views), `static/css/`, `schema.sql`, `data.sql`.

## Expected domain

| Relation | Detail |
|----------|--------|
| School 1—N Class | `SchoolMag` → `SchoolClass` (`school_uuid`) |
| School 1—N User | `SchoolMag` → `User` (`school_uuid`) |
| User N—1 Role | `User` → `Role` (`role_uuid`) |
| Roles | `ADMIN`, `PRINCIPAL`, `TEACHER`, `STUDENT`, `STAFF` |
| Optional | User → SchoolClass (`class_uuid`) |

## Must-verify files

1. `application.yml` — `jdbc:postgresql://…`, not `r2dbc:`
2. `schema.sql` — FK order: roles → school_management → school_classes → users
3. `entities/*` — matches schema columns
4. Controllers under `controller/api` and `controller/view` only
5. `data.sql` / seeder — role names + admin user

## Quick greps

```text
r2dbc:                    → P0 if used with JPA
package ...controller;    → P1 if API/view still in flat controller package
@JoinColumn               → FK matches schema
```

## Output

Use cheap-project-check verdict table. Include **Relations** and **Layout** (api vs view).
