-- Explicit deployment only; never run automatically on application startup.
CREATE TABLE IF NOT EXISTS job_ai_analysis (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  job_id BIGINT NOT NULL,
  input_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  pipeline_version VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  model_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  policy_version VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  state VARCHAR(24) NOT NULL DEFAULT 'pending',
  source_json JSON NOT NULL,
  result_json JSON NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME(6) NULL,
  lease_until DATETIME(6) NULL,
  fence BIGINT NOT NULL DEFAULT 0,
  error_code VARCHAR(64) NULL,
  input_tokens BIGINT NULL,
  output_tokens BIGINT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_job_ai_identity (job_id,input_digest,pipeline_version,model_id,policy_version),
  KEY ix_job_ai_lease (state,next_retry_at,lease_until),
  KEY ix_job_ai_read (job_id,updated_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
