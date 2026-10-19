UPDATE evidence
SET source = 'UNKNOWN'
WHERE source IS NULL OR TRIM(source) = '';

ALTER TABLE evidence
    CHANGE COLUMN source evidence_source VARCHAR(50) NOT NULL,
    CHANGE COLUMN confidence_score confidence DOUBLE,
    ADD COLUMN file_path VARCHAR(500) NULL AFTER evidence_type,
    ADD COLUMN line_range VARCHAR(50) NULL AFTER file_path,
    ADD COLUMN class_name VARCHAR(255) NULL AFTER line_range,
    ADD COLUMN method_name VARCHAR(255) NULL AFTER class_name,
    ADD COLUMN annotation_name VARCHAR(255) NULL AFTER method_name;
