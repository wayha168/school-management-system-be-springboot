INSERT INTO roles (uuid, name, description, created_at)
VALUES
    (gen_random_uuid(), 'SUPERADMIN', 'Platform super administrator', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ADMIN', 'School administrator', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'PRINCIPAL', 'School principal', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'TEACHER', 'Teaching staff', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'STUDENT', 'Enrolled student', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'STAFF', 'Non-teaching staff', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;
