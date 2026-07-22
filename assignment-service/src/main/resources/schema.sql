CREATE TABLE IF NOT EXISTS class_meetings (
    uuid UUID PRIMARY KEY,
    class_uuid UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    room_code VARCHAR(16) NOT NULL UNIQUE,
    meeting_url VARCHAR(1000) NOT NULL,
    provider VARCHAR(20) NOT NULL DEFAULT 'NATIVE',
    record_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    has_recording BOOLEAN NOT NULL DEFAULT FALSE,
    recording_stored_name VARCHAR(255),
    recording_content_type VARCHAR(100),
    recording_bytes BIGINT,
    recorded_at TIMESTAMP,
    scheduled_at TIMESTAMP,
    created_by UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_class_meetings_class ON class_meetings (class_uuid);
CREATE INDEX IF NOT EXISTS idx_class_meetings_active ON class_meetings (class_uuid, active);
CREATE INDEX IF NOT EXISTS idx_class_meetings_room ON class_meetings (room_code);

-- Safe upgrades for existing databases (continue-on-error)
ALTER TABLE class_meetings ADD COLUMN IF NOT EXISTS room_code VARCHAR(16);
ALTER TABLE class_meetings ADD COLUMN IF NOT EXISTS record_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE class_meetings ADD COLUMN IF NOT EXISTS has_recording BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE class_meetings ADD COLUMN IF NOT EXISTS recording_stored_name VARCHAR(255);
ALTER TABLE class_meetings ADD COLUMN IF NOT EXISTS recording_content_type VARCHAR(100);
ALTER TABLE class_meetings ADD COLUMN IF NOT EXISTS recording_bytes BIGINT;
ALTER TABLE class_meetings ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMP;

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
    content TEXT,
    has_attachment BOOLEAN NOT NULL DEFAULT FALSE,
    attachment_stored_name VARCHAR(255),
    attachment_original_name VARCHAR(255),
    attachment_content_type VARCHAR(120),
    attachment_bytes BIGINT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assignment_student UNIQUE (assignment_uuid, student_uuid)
);

CREATE INDEX IF NOT EXISTS idx_submissions_assignment ON assignment_submissions (assignment_uuid);

ALTER TABLE assignment_submissions ALTER COLUMN content DROP NOT NULL;
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS has_attachment BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS attachment_stored_name VARCHAR(255);
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS attachment_original_name VARCHAR(255);
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS attachment_content_type VARCHAR(120);
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS attachment_bytes BIGINT;
