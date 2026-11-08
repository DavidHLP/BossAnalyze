-- Optional compatibility migration for environments that used legacy created_at.
-- Standard system.sql already defines create_time and needs no schema change.
DELIMITER $$
CREATE PROCEDURE bossanalyze_align_token_time()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'token' AND column_name = 'create_time') THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'token' AND column_name = 'created_at') THEN
      ALTER TABLE token RENAME COLUMN created_at TO create_time;
    ELSE
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing token timestamp column: inspect schema manually';
    END IF;
  END IF;
END$$
DELIMITER ;
CALL bossanalyze_align_token_time();
DROP PROCEDURE bossanalyze_align_token_time;
-- No token records or timestamp values are removed/rewritten.
