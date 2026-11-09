-- Explicit operator migration for MySQL 8; select the target schema first.
-- Back up and rehearse first: docs/operations/database-role-uniqueness.md.
-- Use mysql batch WITHOUT --force: any failed guard must stop the file.
-- DDL implicitly commits; lock timeout bounds waiting, not total execution time.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

-- Reject dirty/ambiguous data even when an index already exists. Compare with
-- the column's own collation; BINARY checks canonical spelling and whitespace.
SET @role_data_ok = (
  NOT EXISTS (SELECT 1 FROM role GROUP BY role_name HAVING COUNT(*) > 1)
  AND NOT EXISTS (SELECT 1 FROM role WHERE role_name IS NULL OR TRIM(role_name) = ''
    OR BINARY role_name <> BINARY TRIM(role_name))
  AND (SELECT COUNT(*) FROM role WHERE role_name = 'USER') = 1
  AND (SELECT COUNT(*) FROM role WHERE BINARY role_name = BINARY 'USER' AND status = 1) = 1
);
SET @role_full_unique = (
  SELECT COUNT(*) FROM (
    SELECT index_name FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'role'
    GROUP BY index_name
    HAVING COUNT(*) = 1 AND MAX(non_unique) = 0
      AND MAX(column_name) = 'role_name' AND MAX(sub_part) IS NULL
      AND MIN(is_visible) = 'YES'
  ) full_indexes
);
SET @role_named_index_bad = (
  SELECT COUNT(*) > 0 FROM (
    SELECT index_name FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'role' AND index_name = 'uk_role_name'
    GROUP BY index_name
    HAVING COUNT(*) <> 1 OR MAX(non_unique) <> 0 OR COALESCE(MAX(column_name), '') <> 'role_name'
      OR MAX(sub_part) IS NOT NULL OR MIN(is_visible) <> 'YES'
  ) bad_indexes
);
-- SIGNAL outside a routine is not preparable on all supported MySQL versions.
-- A deliberately invalid statement fails PREPARE before any ALTER on guard failure.
SET @role_ddl = CASE
  WHEN NOT @role_data_ok THEN 'ABORT MIGRATION dirty_or_ambiguous_role_data'
  WHEN @role_named_index_bad THEN 'ABORT MIGRATION wrong_uk_role_name_definition'
  WHEN @role_full_unique > 0 THEN 'SELECT ''role_name full unique index already present: skipped'''
  ELSE 'ALTER TABLE role ADD UNIQUE KEY uk_role_name (role_name), ALGORITHM=INPLACE, LOCK=NONE'
END;
PREPARE role_constraint FROM @role_ddl;
EXECUTE role_constraint;
DEALLOCATE PREPARE role_constraint;
