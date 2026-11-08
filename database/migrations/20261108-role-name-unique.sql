-- Apply to the selected existing schema; NEVER import system.sql into a live database.
-- Read-only audit. Resolve duplicates explicitly; this migration never rewrites/deletes rows.
SELECT role_name, COUNT(*) AS duplicates FROM role GROUP BY role_name HAVING COUNT(*) > 1;
DELIMITER $$
CREATE PROCEDURE bossanalyze_add_role_name_constraint()
BEGIN
  IF EXISTS (SELECT 1 FROM role GROUP BY role_name HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate role names: resolve manually before migration';
  END IF;
  -- Atomic DDL rechecks concurrent duplicates and arbitrates future concurrent inserts.
  ALTER TABLE role ADD UNIQUE KEY uk_role_name (role_name);
END$$
DELIMITER ;
CALL bossanalyze_add_role_name_constraint();
DROP PROCEDURE bossanalyze_add_role_name_constraint;
-- If CALL aborts, drop this helper procedure before retrying after a reviewed data correction.
-- TokenMapper reads existing create_time; no token data migration is needed.
