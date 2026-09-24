-- Fix the seeded admin password.
--
-- V5 inserted a placeholder BCrypt hash that does not correspond to the
-- documented password (`Admin@1234`). Logging in as `admin` therefore always
-- failed with 401 "Invalid username or password".
--
-- The value below is a BCrypt hash (cost 12, matching BCryptPasswordEncoder(12))
-- of `Admin@1234`. Verified by AdminSeedPasswordTest.
UPDATE users
SET password_hash = '$2a$12$5y3VOLS84wB8MuUfOIDwluBPlNiBsUTOR/KsNDsDH9AafXCT5iXBi'
WHERE username = 'admin';
