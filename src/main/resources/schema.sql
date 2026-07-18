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
    logo VARCHAR(255) NOT NULL,
    banner VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS school_classes (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    grade VARCHAR(50),
    school_uuid UUID NOT NULL REFERENCES school_management (uuid),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_uuid UUID NOT NULL REFERENCES roles (uuid),
    school_uuid UUID NOT NULL REFERENCES school_management (uuid),
    class_uuid UUID REFERENCES school_classes (uuid),
    grade VARCHAR(50),
    room VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS grade VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS room VARCHAR(100);

CREATE TABLE IF NOT EXISTS role_permissions (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_uuid UUID NOT NULL REFERENCES roles (uuid) ON DELETE CASCADE,
    permission VARCHAR(100) NOT NULL,
    CONSTRAINT uq_role_permission UNIQUE (role_uuid, permission)
);

CREATE INDEX IF NOT EXISTS idx_users_school_uuid ON users (school_uuid);
CREATE INDEX IF NOT EXISTS idx_users_role_uuid ON users (role_uuid);
CREATE INDEX IF NOT EXISTS idx_school_classes_school_uuid ON school_classes (school_uuid);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_uuid ON role_permissions (role_uuid);
