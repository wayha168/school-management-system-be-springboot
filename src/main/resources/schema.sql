-- Canonical PostgreSQL schema for School Management.
-- Runs on startup (spring.sql.init.mode=always) with continue-on-error.
-- Hibernate ddl-auto=update also syncs entity changes; keep this file aligned
-- whenever new tables/columns are added so fresh DBs match the app.

CREATE TABLE IF NOT EXISTS roles (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Hibernate may leave a stale enum CHECK that blocks new roles (e.g. SUPERADMIN)
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check;

CREATE TABLE IF NOT EXISTS school_management (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    website VARCHAR(255) NOT NULL,
    logo_data BYTEA,
    logo_content_type VARCHAR(100),
    banner_data BYTEA,
    banner_content_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Migrate older installs that stored logo/banner as URL strings
ALTER TABLE school_management ADD COLUMN IF NOT EXISTS logo_data BYTEA;
ALTER TABLE school_management ADD COLUMN IF NOT EXISTS logo_content_type VARCHAR(100);
ALTER TABLE school_management ADD COLUMN IF NOT EXISTS banner_data BYTEA;
ALTER TABLE school_management ADD COLUMN IF NOT EXISTS banner_content_type VARCHAR(100);
ALTER TABLE school_management ALTER COLUMN logo DROP NOT NULL;
ALTER TABLE school_management ALTER COLUMN banner DROP NOT NULL;
ALTER TABLE school_management DROP COLUMN IF EXISTS logo;
ALTER TABLE school_management DROP COLUMN IF EXISTS banner;

CREATE TABLE IF NOT EXISTS school_classes (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    grade VARCHAR(50),
    generation INTEGER NOT NULL DEFAULT 1,
    academic_year INTEGER,
    school_uuid UUID NOT NULL REFERENCES school_management (uuid),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE school_classes ADD COLUMN IF NOT EXISTS generation INTEGER;
ALTER TABLE school_classes ADD COLUMN IF NOT EXISTS academic_year INTEGER;
ALTER TABLE school_classes ADD COLUMN IF NOT EXISTS grade VARCHAR(50);

CREATE TABLE IF NOT EXISTS school_class_subjects (
    school_class_uuid UUID NOT NULL REFERENCES school_classes (uuid) ON DELETE CASCADE,
    subject VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (school_class_uuid, sort_order)
);

CREATE TABLE IF NOT EXISTS users (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_uuid UUID NOT NULL REFERENCES roles (uuid),
    school_uuid UUID NOT NULL REFERENCES school_management (uuid),
    grade VARCHAR(50),
    room VARCHAR(100),
    salary NUMERIC(14, 2),
    profile_image_data BYTEA,
    profile_image_content_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS grade VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS room VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS salary NUMERIC(14, 2);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_data BYTEA;
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_content_type VARCHAR(100);
-- Legacy single-class FK replaced by user_school_classes join table
ALTER TABLE users DROP COLUMN IF EXISTS class_uuid;

CREATE TABLE IF NOT EXISTS user_school_classes (
    user_uuid UUID NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    school_class_uuid UUID NOT NULL REFERENCES school_classes (uuid) ON DELETE CASCADE,
    PRIMARY KEY (user_uuid, school_class_uuid)
);

-- student_scores moved to assessment-service DB (school-assessment)

CREATE TABLE IF NOT EXISTS role_permissions (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_uuid UUID NOT NULL REFERENCES roles (uuid) ON DELETE CASCADE,
    permission VARCHAR(100) NOT NULL,
    CONSTRAINT uq_role_permission UNIQUE (role_uuid, permission)
);

CREATE TABLE IF NOT EXISTS attendance_records (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_uuid UUID NOT NULL REFERENCES users (uuid),
    school_class_uuid UUID REFERENCES school_classes (uuid),
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    marked_by_uuid UUID REFERENCES users (uuid),
    remark VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_requests (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_user_uuid UUID NOT NULL REFERENCES users (uuid),
    subject VARCHAR(200) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    category VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    admin_reply VARCHAR(2000),
    handled_by_uuid UUID REFERENCES users (uuid),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payroll_records (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_uuid UUID NOT NULL REFERENCES users (uuid),
    period VARCHAR(7) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(255),
    created_by_uuid UUID REFERENCES users (uuid),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_records (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_uuid UUID NOT NULL REFERENCES users (uuid),
    payment_type VARCHAR(30) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    due_date DATE,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(255),
    created_by_uuid UUID REFERENCES users (uuid),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_school_uuid ON users (school_uuid);
CREATE INDEX IF NOT EXISTS idx_users_role_uuid ON users (role_uuid);
CREATE INDEX IF NOT EXISTS idx_school_classes_school_uuid ON school_classes (school_uuid);
CREATE INDEX IF NOT EXISTS idx_school_classes_generation ON school_classes (generation);
CREATE INDEX IF NOT EXISTS idx_school_classes_grade ON school_classes (grade);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_uuid ON role_permissions (role_uuid);
CREATE INDEX IF NOT EXISTS idx_user_school_classes_user ON user_school_classes (user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_school_classes_class ON user_school_classes (school_class_uuid);
CREATE INDEX IF NOT EXISTS idx_attendance_user ON attendance_records (user_uuid);
CREATE INDEX IF NOT EXISTS idx_attendance_class ON attendance_records (school_class_uuid);
CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance_records (attendance_date);
CREATE INDEX IF NOT EXISTS idx_user_requests_from ON user_requests (from_user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_requests_status ON user_requests (status);
CREATE INDEX IF NOT EXISTS idx_payroll_user ON payroll_records (user_uuid);
CREATE INDEX IF NOT EXISTS idx_payment_user ON payment_records (user_uuid);
