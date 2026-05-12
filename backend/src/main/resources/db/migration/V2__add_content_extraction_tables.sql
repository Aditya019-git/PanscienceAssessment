ALTER TABLE files
    ADD COLUMN processing_error TEXT;

CREATE TABLE chunks (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    chunk_text TEXT NOT NULL,
    page_number INTEGER,
    start_time DOUBLE PRECISION,
    end_time DOUBLE PRECISION,
    embedding_ref VARCHAR(255)
);

CREATE TABLE transcript_segments (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    segment_text TEXT NOT NULL,
    start_time DOUBLE PRECISION NOT NULL,
    end_time DOUBLE PRECISION NOT NULL,
    sequence_number INTEGER NOT NULL
);
