#!/bin/sh
# Runs once, when the database volume is first initialized (official postgres image convention).
# Creates the role the application connects with. Flyway (connecting as the owner, POSTGRES_USER) then grants
# it data access only: no DDL, no UPDATE/DELETE on the append-only tables (see V3__security_hardening.sql).
set -eu
psql -v ON_ERROR_STOP=1 \
     -v app_user="$APP_DB_USER" -v app_password="$APP_DB_PASSWORD" -v db_name="$POSTGRES_DB" \
     --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'SQL'
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password';
GRANT CONNECT ON DATABASE :"db_name" TO :"app_user";
SQL
