#!/bin/bash
# Script de configuration de la base PostgreSQL locale pour MediLinkPro.
# Aligne l'utilisateur/mot de passe/base sur les valeurs par defaut
# definies dans application.yml et docker-compose.yml.
#
# Usage : ./setup-db.sh
# Necessite que PostgreSQL soit installe et le service demarre en local
# (pas via Docker). Pour Docker, utilisez `docker compose up -d` a la place.

set -e

DB_USER="${DB_USERNAME:-medilinkpro_user}"
DB_PASS="${DB_PASSWORD:-medilinkpro_pass}"
DB_NAME="${DB_NAME:-medilinkpro}"

echo "Configuration de PostgreSQL pour MediLinkPro..."
echo "  Utilisateur : $DB_USER"
echo "  Base        : $DB_NAME"
echo

# Cree l'utilisateur s'il n'existe pas, sinon met a jour son mot de passe
sudo -u postgres psql -v ON_ERROR_STOP=1 <<EOF
DO \$\$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${DB_USER}') THEN
      CREATE ROLE ${DB_USER} WITH LOGIN PASSWORD '${DB_PASS}';
   ELSE
      ALTER ROLE ${DB_USER} WITH PASSWORD '${DB_PASS}';
   END IF;
END
\$\$;
EOF

# Cree la base si elle n'existe pas
sudo -u postgres psql -v ON_ERROR_STOP=1 -tc "SELECT 1 FROM pg_database WHERE datname = '${DB_NAME}'" | grep -q 1 || \
  sudo -u postgres psql -v ON_ERROR_STOP=1 -c "CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};"

sudo -u postgres psql -v ON_ERROR_STOP=1 -c "GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};"

echo
echo "Termine. Vous pouvez maintenant lancer : mvn spring-boot:run"
