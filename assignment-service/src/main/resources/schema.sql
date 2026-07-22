CREATE TABLE IF NOT EXISTS class_meetings (
    uuid UUID PRIMARY KEY,
    class_uuid UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    meeting_url VARCHAR(1000) NOT NULL,
    provider VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    scheduled_at TIMESTAMP,
    created_by UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_class_meetings_class ON class_meetings (class_uuid);
CREATE INDEX IF NOT EXISTS idx_class_meetings_active ON class_meetings (class_uuid, active);

CREATE TABLE IF NOT EXISTS assignments (
    uuid UUID PRIMARY KEY,
    class_uuid UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    due_at TIMESTAMP,
    created_by UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_assignments_class ON assignments (class_uuid);
CREATE INDEX IF NOT EXISTS idx_assignments_status ON assignments (class_uuid, status);

CREATE TABLE IF NOT EXISTS assignment_submissions (
    uuid UUID PRIMARY KEY,
    assignment_uuid UUID NOT NULL REFERENCES assignments (uuid) ON DELETE CASCADE,
    student_uuid UUID NOT NULL,
    content TEXT NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assignment_student UNIQUE (assignment_uuid, student_uuid)
);

CREATE INDEX IF NOT EXISTS idx_submissions_assignment ON assignment_submissions (assignment_uuid);
