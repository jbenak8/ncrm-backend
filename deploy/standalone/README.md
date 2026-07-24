# Standalone deployment (db-auth profile)

Runs the nCRM backend as a self-contained background service with **database authentication**
(`db-auth` Spring profile) – no Keycloak, MailHog or bundled PostgreSQL. The application connects
to an externally managed PostgreSQL instance and SMTP server and issues its own JWT access tokens.

## Docker (backend only)

From the project root:

```bash
docker compose -f docker-compose.db-auth.yaml up --build -d
```

The compose file uses `Dockerfile.db-auth` and requires the connection and secret variables
(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_HOST`, `NCRM_JWT_SECRET`, ...) – provide them
via an `.env` file next to the compose file or via the shell environment.

## Linux (systemd service)

```bash
sudo deploy/standalone/build-and-install.sh
sudo nano /etc/ncrm-backend/ncrm-backend.env   # fill in database, SMTP and NCRM_JWT_SECRET
sudo systemctl start ncrm-backend
journalctl -u ncrm-backend -f                  # logs
```

The script builds the jar with the Maven wrapper, creates the system user `ncrm`, installs the
application to `/opt/ncrm-backend`, the configuration to `/etc/ncrm-backend/ncrm-backend.env`
and registers a hardened systemd unit that starts automatically on boot.

Uninstall: `sudo systemctl disable --now ncrm-backend && sudo rm /etc/systemd/system/ncrm-backend.service`

## Windows (Windows service)

From an elevated (Administrator) PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File deploy\standalone\build-and-install.ps1
notepad "C:\Program Files\ncrm-backend\ncrm-backend.env"   # fill in database, SMTP and NCRM_JWT_SECRET
powershell -ExecutionPolicy Bypass -File deploy\standalone\build-and-install.ps1   # re-run to apply the env file
Start-Service ncrm-backend
```

The script builds the jar, installs it to `C:\Program Files\ncrm-backend` (override with
`-InstallDir`), downloads the [WinSW](https://github.com/winsw/winsw) service wrapper and
registers the `ncrm-backend` Windows service (automatic start, restart on failure,
rolling log files in the `logs` subdirectory).

Uninstall: `powershell -ExecutionPolicy Bypass -File deploy\standalone\build-and-install.ps1 -Uninstall`

## Configuration

All settings are provided through environment variables – see `ncrm-backend.env.example`.
`NCRM_JWT_SECRET` must be a random string of at least 32 bytes, e.g. `openssl rand -base64 48`.
Keep the env file readable only by the service account, it contains secrets.

Prerequisites for the service installation: Java (JDK/JRE 26+) available on the `PATH`
of the target machine; Docker is not required for the service variant.
