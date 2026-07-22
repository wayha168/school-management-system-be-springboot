# Default stack: school-management (8080) + assessment (8081) + assignment (8082)
# Both microservices use Windows/host Postgres via host.docker.internal.
#
# Start everything:
#   docker compose up --build
#
# Assessment + assignment only (run main app in IDE):
#   docker compose up --build assessment-service assignment-service
#
# Optional Postgres+Redis (empty DB — not your Windows data):
#   docker compose -f compose.infra.yaml up -d
#
# Required host databases:
#   school-management
#   school-assessment
#   school-assignment
#
# Classroom video: create call on class detail (no Meet/Zoom URL).
# Join path: /admin/classroom/call/{roomCode}
# Recordings volume: assignment-recordings → /data/recordings
