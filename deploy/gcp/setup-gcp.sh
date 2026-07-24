#!/usr/bin/env bash
# One-time setup of the Google Cloud project for the nCRM backend:
# enables the required APIs, creates the Artifact Registry repository, the Cloud SQL
# (PostgreSQL) instance, the runtime service account and the Secret Manager secrets.
# Usage: PROJECT_ID=my-project REGION=europe-west1 ./deploy/gcp/setup-gcp.sh
set -euo pipefail

PROJECT_ID="${PROJECT_ID:?Set PROJECT_ID, e.g. PROJECT_ID=my-project $0}"
REGION="${REGION:-europe-west1}"
REPOSITORY="${REPOSITORY:-ncrm}"
SQL_INSTANCE="${SQL_INSTANCE:-ncrm-postgres}"
SERVICE_ACCOUNT="ncrm-backend@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud config set project "${PROJECT_ID}"

echo "==> Enabling required APIs"
gcloud services enable run.googleapis.com cloudbuild.googleapis.com \
  artifactregistry.googleapis.com sqladmin.googleapis.com secretmanager.googleapis.com

echo "==> Creating Artifact Registry repository '${REPOSITORY}'"
gcloud artifacts repositories describe "${REPOSITORY}" --location "${REGION}" >/dev/null 2>&1 ||
  gcloud artifacts repositories create "${REPOSITORY}" \
    --repository-format docker --location "${REGION}" \
    --description "nCRM container images"

echo "==> Creating Cloud SQL (PostgreSQL) instance '${SQL_INSTANCE}'"
gcloud sql instances describe "${SQL_INSTANCE}" >/dev/null 2>&1 ||
  gcloud sql instances create "${SQL_INSTANCE}" \
    --database-version POSTGRES_17 --region "${REGION}" \
    --tier db-custom-1-3840 --storage-auto-increase
gcloud sql databases describe nCRM --instance "${SQL_INSTANCE}" >/dev/null 2>&1 ||
  gcloud sql databases create nCRM --instance "${SQL_INSTANCE}"
gcloud sql users list --instance "${SQL_INSTANCE}" --format 'value(name)' | grep -qx ncrm ||
  gcloud sql users create ncrm --instance "${SQL_INSTANCE}" \
    --password "$(openssl rand -base64 24)" # change afterwards, store in the secret below

echo "==> Creating runtime service account"
gcloud iam service-accounts describe "${SERVICE_ACCOUNT}" >/dev/null 2>&1 ||
  gcloud iam service-accounts create ncrm-backend --display-name "nCRM backend (Cloud Run)"
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member "serviceAccount:${SERVICE_ACCOUNT}" --role roles/cloudsql.client --condition None
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member "serviceAccount:${SERVICE_ACCOUNT}" --role roles/secretmanager.secretAccessor --condition None

echo "==> Creating Secret Manager secrets (empty placeholders unless they already exist)"
for secret in ncrm-db-username ncrm-db-password ncrm-mail-username ncrm-mail-password \
              ncrm-anthropic-api-key ncrm-openai-api-key; do
  gcloud secrets describe "${secret}" >/dev/null 2>&1 ||
    printf 'change-me' | gcloud secrets create "${secret}" --data-file -
done

cat <<EOF

Setup finished. Next steps:
  1. Store the real values in Secret Manager, e.g.:
       printf 'my-password' | gcloud secrets versions add ncrm-db-password --data-file -
  2. Edit deploy/gcp/ncrm-backend-service.yaml (PROJECT_ID, REGION, Keycloak, SMTP, CORS).
  3. Build & deploy from the project root:
       gcloud builds submit --config deploy/gcp/cloudbuild.yaml
EOF
