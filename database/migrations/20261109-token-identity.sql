-- Explicit MySQL 8 migration; select the schema, back up and rehearse first.
-- Run mysql batch WITHOUT --force. DDL commits implicitly; stop writers first.
-- No rows/sessions are deleted or revoked. See README-token-identity.md.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SET @token_column_ok = (
  SELECT COUNT(*) = 1 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'token' AND column_name = 'token'
    AND data_type = 'varchar' AND is_nullable = 'NO' AND column_default IS NULL AND extra = ''
    AND ((character_maximum_length = 255 AND character_set_name = 'utf8mb4' AND collation_name = 'utf8mb4_0900_ai_ci')
      OR (character_maximum_length = 2560 AND character_set_name = 'ascii' AND collation_name = 'ascii_bin'))
);
SET @token_column_ready = (
  SELECT COUNT(*) = 1 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'token' AND column_name = 'token'
    AND character_maximum_length = 2560 AND character_set_name = 'ascii' AND collation_name = 'ascii_bin'
);
SET @token_table_ok = (
  VERSION() LIKE '8.%' AND @@innodb_page_size >= 16384 AND
  (SELECT COUNT(*) = 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'token' AND engine = 'InnoDB' AND row_format = 'Dynamic')
  AND NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage
    WHERE (table_schema = DATABASE() AND table_name = 'token' AND column_name = 'token' AND referenced_table_name IS NOT NULL)
       OR (referenced_table_schema = DATABASE() AND referenced_table_name = 'token' AND referenced_column_name = 'token'))
);
-- Compare bytes before conversion; reject non-ASCII, trailing spaces (ascii_bin
-- is PAD SPACE), over-capacity values and duplicates, including revoked rows.
SET @token_data_ok = (
  NOT EXISTS (SELECT 1 FROM token WHERE token IS NULL OR OCTET_LENGTH(token) <> CHAR_LENGTH(token)
    OR OCTET_LENGTH(token) > 2560 OR OCTET_LENGTH(token) <> OCTET_LENGTH(RTRIM(token)))
  AND NOT EXISTS (SELECT 1 FROM token GROUP BY CAST(token AS BINARY) HAVING COUNT(*) > 1)
);
SET @token_full_unique = (
  SELECT COUNT(*) FROM (
    SELECT index_name FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'token'
    GROUP BY index_name
    HAVING COUNT(*) = 1 AND MAX(non_unique) = 0 AND MAX(column_name) = 'token'
      AND MAX(sub_part) IS NULL AND MIN(is_visible) = 'YES' AND MIN(index_type) = 'BTREE'
  ) full_indexes
);
-- Reject unknown same-name indexes and incompatible token indexes before any DDL.
SET @token_index_bad = (
  SELECT COUNT(*) > 0 FROM (
    SELECT index_name FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'token'
    GROUP BY index_name
    HAVING (index_name = 'uk_token_value' OR SUM(column_name = 'token') > 0)
      AND (COUNT(*) <> 1 OR MAX(non_unique) <> 0 OR COALESCE(MAX(column_name), '') <> 'token'
        OR MAX(sub_part) IS NOT NULL OR MIN(is_visible) <> 'YES' OR MIN(index_type) <> 'BTREE')
  ) bad_indexes
);
-- As in the role migration, invalid PREPARE aborts before ALTER; no routine needed.
-- One ALTER makes partial column/index application impossible within this statement.
SET @token_ddl = CASE
  WHEN NOT @token_column_ok OR NOT @token_table_ok THEN 'ABORT MIGRATION incompatible_token_structure'
  WHEN NOT @token_data_ok THEN 'ABORT MIGRATION incompatible_or_duplicate_token_data'
  WHEN @token_index_bad THEN 'ABORT MIGRATION wrong_token_index_definition'
  WHEN @token_column_ready AND @token_full_unique > 0 THEN 'SELECT ''token identity schema already present: skipped'''
  WHEN @token_column_ready THEN 'ALTER TABLE token ADD UNIQUE KEY uk_token_value (token)'
  WHEN @token_full_unique > 0 THEN 'ALTER TABLE token MODIFY COLUMN token VARCHAR(2560) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT ''Token值'''
  ELSE 'ALTER TABLE token MODIFY COLUMN token VARCHAR(2560) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT ''Token值'', ADD UNIQUE KEY uk_token_value (token)'
END;
PREPARE token_identity FROM @token_ddl;
EXECUTE token_identity;
DEALLOCATE PREPARE token_identity;
