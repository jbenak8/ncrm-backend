#!/usr/bin/env bash
# Builds the nCRM backend and installs it as a background systemd service on Linux
# (standalone "db-auth" deployment: no Keycloak, MailHog or bundled PostgreSQL).
#
# Usage (run from anywhere, requires root for the installation part):
#   sudo deploy/standalone/build-and-install.sh
#
# What it does:
#   1. Builds the executable jar with the Maven wrapper (tests skipped).
#   2. Creates the system user "ncrm" and the directories /opt/ncrm-backend and /etc/ncrm-backend.
#   3. Installs the jar, the environment file template and the systemd unit "ncrm-backend.service".
#   4. Enables the service so it starts automatically on boot.
#
# After the installation, edit /etc/ncrm-backend/ncrm-backend.env (database, SMTP, NCRM_JWT_SECRET)
# and start the service with: sudo systemctl start ncrm-backend
set -euo pipefail

SERVICE_NAME="ncrm-backend"
INSTALL_DIR="/opt/${SERVICE_NAME}"
CONFIG_DIR="/etc/${SERVICE_NAME}"
SERVICE_USER="ncrm"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "==> Building ${SERVICE_NAME} (tests skipped)..."
(cd "${PROJECT_DIR}" && ./mvnw -q package -DskipTests)

JAR_FILE="$(ls "${PROJECT_DIR}"/target/*.jar | grep -v -E '(sources|javadoc)\.jar$' | head -n 1)"
if [[ -z "${JAR_FILE}" ]]; then
    echo "ERROR: no jar found in ${PROJECT_DIR}/target" >&2
    exit 1
fi
echo "==> Built ${JAR_FILE}"

if [[ "${EUID}" -ne 0 ]]; then
    echo "ERROR: installation requires root privileges, re-run with sudo." >&2
    exit 1
fi

echo "==> Creating service user and directories..."
id -u "${SERVICE_USER}" &>/dev/null || useradd --system --no-create-home --shell /usr/sbin/nologin "${SERVICE_USER}"
install -d -o "${SERVICE_USER}" -g "${SERVICE_USER}" "${INSTALL_DIR}"
install -d -m 750 -o root -g "${SERVICE_USER}" "${CONFIG_DIR}"

echo "==> Installing application jar..."
install -o "${SERVICE_USER}" -g "${SERVICE_USER}" -m 644 "${JAR_FILE}" "${INSTALL_DIR}/${SERVICE_NAME}.jar"

if [[ ! -f "${CONFIG_DIR}/${SERVICE_NAME}.env" ]]; then
    echo "==> Installing environment file template to ${CONFIG_DIR}/${SERVICE_NAME}.env"
    install -o root -g "${SERVICE_USER}" -m 640 "${SCRIPT_DIR}/${SERVICE_NAME}.env.example" "${CONFIG_DIR}/${SERVICE_NAME}.env"
else
    echo "==> Keeping existing ${CONFIG_DIR}/${SERVICE_NAME}.env"
fi

echo "==> Installing systemd unit..."
cat > "/etc/systemd/system/${SERVICE_NAME}.service" <<EOF
[Unit]
Description=nCRM backend (standalone, database authentication)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=${SERVICE_USER}
Group=${SERVICE_USER}
EnvironmentFile=${CONFIG_DIR}/${SERVICE_NAME}.env
WorkingDirectory=${INSTALL_DIR}
ExecStart=/usr/bin/env java -XX:MaxRAMPercentage=75 -jar ${INSTALL_DIR}/${SERVICE_NAME}.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

# Hardening
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=${INSTALL_DIR}

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable "${SERVICE_NAME}"

echo
echo "Installation finished."
echo "1. Edit ${CONFIG_DIR}/${SERVICE_NAME}.env (database, SMTP, NCRM_JWT_SECRET)."
echo "2. Start the service:   sudo systemctl start ${SERVICE_NAME}"
echo "3. Check the status:    systemctl status ${SERVICE_NAME}"
echo "4. Follow the logs:     journalctl -u ${SERVICE_NAME} -f"
