-- Assessment service schema (scores + GPA access grants)

CREATE TABLE IF NOT EXISTS student_scores (
    uuid UUID PRIMARY KEY,
    student_uuid UUID NOT NULL,
    student_name VARCHAR(150),
    student_email VARCHAR(150),
    student_grade VARCHAR(50),
    school_uuid UUID,
    school_class_uuid UUID NOT NULL,
    class_name VARCHAR(150),
    generation INTEGER,
    academic_year INTEGER,
    teacher_uuid UUID NOT NULL,
    teacher_name VARCHAR(150),
    subject VARCHAR(100) NOT NULL,
    term VARCHAR(50) NOT NULL,
    score NUMERIC(6, 2) NOT NULL,
    max_score NUMERIC(6, 2) NOT NULL,
    remark VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_score_student_class_subject_term
        UNIQUE (student_uuid, school_class_uuid, subject, term)
);

CREATE INDEX IF NOT EXISTS idx_scores_class ON student_scores (school_class_uuid);
CREATE INDEX IF NOT EXISTS idx_scores_student ON student_scores (student_uuid);
CREATE INDEX IF NOT EXISTS idx_scores_teacher ON student_scores (teacher_uuid);
CREATE INDEX IF NOT EXISTS idx_scores_school ON student_scores (school_uuid);
CREATE INDEX IF NOT EXISTS idx_scores_generation ON student_scores (generation);

CREATE TABLE IF NOT EXISTS gpa_access_grants (
    student_uuid UUID PRIMARY KEY,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by_uuid UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
