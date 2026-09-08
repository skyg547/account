-- ==============================================================================
-- Baseline Test Data for Auth Local H2 Profile
-- ==============================================================================
-- Default Password: Test1234!
-- BCrypt Hash: {bcrypt}$2a$10$mGpS8ZLVkXW1MnmUccJxIe5v9LtQWTcsD4xAfaKCTeVuTIAZ4sCh2 (cost 10)
-- Schema: V70__auth_user_role_schema.sql
-- ==============================================================================

DELETE FROM auth_role_assignments;
DELETE FROM auth_users;

-- 1. Baseline Users
INSERT INTO auth_users (username, stored_password, department_code, account_active, account_locked, role_version, created_at, updated_at)
VALUES
    ('admin', '{bcrypt}$2a$10$mGpS8ZLVkXW1MnmUccJxIe5v9LtQWTcsD4xAfaKCTeVuTIAZ4sCh2', 'DEPT_ADMIN', true, false, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('auditor', '{bcrypt}$2a$10$mGpS8ZLVkXW1MnmUccJxIe5v9LtQWTcsD4xAfaKCTeVuTIAZ4sCh2', 'DEPT_AUDIT', true, false, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('user', '{bcrypt}$2a$10$mGpS8ZLVkXW1MnmUccJxIe5v9LtQWTcsD4xAfaKCTeVuTIAZ4sCh2', 'DEPT_OPS', true, false, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. Baseline Role Assignments (ROLE_ADMIN, ROLE_USER, ROLE_AUDITOR, ROLE_FINANCE)
INSERT INTO auth_role_assignments (username, role_code, data_scope, valid_from, valid_to, approved, approved_by, approved_at, created_at)
VALUES
    ('admin', 'ROLE_ADMIN', 'GLOBAL', NULL, NULL, true, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('admin', 'ROLE_USER', 'GLOBAL', NULL, NULL, true, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('admin', 'ROLE_FINANCE', 'GLOBAL', NULL, NULL, true, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('auditor', 'ROLE_AUDITOR', 'GLOBAL', NULL, NULL, true, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('auditor', 'ROLE_USER', 'GLOBAL', NULL, NULL, true, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('user', 'ROLE_USER', 'GLOBAL', NULL, NULL, true, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
