-- Default admin account
-- Password: admin123 (BCrypt hash)
INSERT INTO utilisateur (id, dtype, salon_id, first_name, last_name, email, password_hash, commission_rate, active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'ADMIN',
    NULL,
    'Super',
    'Admin',
    'admin@coiflow.com',
    '$2a$10$qPHDq5RGMG.UWl2MSEhWIu5RLQh/uOKLnS3pABH6G/5qXTCWrpaLa',
    NULL,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
