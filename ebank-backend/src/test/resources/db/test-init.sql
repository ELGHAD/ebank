-- Runs once when the test PostgreSQL container starts, as the container's superuser (which also runs Flyway).
-- The application connects as this restricted role, exactly like in Docker Compose.
create role ebank_app login password 'ebank_app_test_password';
