# Google Cloud deployment (Cloud Run)

Runs the nCRM backend as a fully managed **Cloud Run** service with a **Cloud SQL for
PostgreSQL** database, secrets in **Secret Manager** and images built by **Cloud Build**
into **Artifact Registry**. The application connects to Cloud SQL through the built-in
connector (the `postgres-socket-factory` dependency is already part of the backend).

## Prerequisites

- A Google Cloud project with billing enabled.
- The [`gcloud` CLI](https://cloud.google.com/sdk/docs/install) authenticated
  (`gcloud auth login`) with permissions to create the resources below.
- An external Keycloak instance (for the `prod` profile) and an SMTP server.

## 1. One-time project setup

From the project root (Cloud Shell or any Linux/macOS shell):

```bash
PROJECT_ID=my-project REGION=europe-west1 ./deploy/gcp/setup-gcp.sh
```

The script enables the required APIs and creates the Artifact Registry repository `ncrm`,
the Cloud SQL instance `ncrm-postgres` with database `nCRM` and user `ncrm`, the runtime
service account `ncrm-backend` (roles `cloudsql.client` and `secretmanager.secretAccessor`)
and placeholder secrets. It is idempotent and can be re-run safely.

Then store the real secret values, for example:

```bash
printf 'ncrm'        | gcloud secrets versions add ncrm-db-username --data-file -
printf 'my-password' | gcloud secrets versions add ncrm-db-password --data-file -
```

## 2. Configure the service

Edit `deploy/gcp/ncrm-backend-service.yaml` and replace `PROJECT_ID`, `REGION` and the
example values of `KEYCLOAK_ISSUER_URI`, `MAIL_HOST`/`MAIL_PORT` and
`NCRM_CORS_ALLOWED_ORIGINS`. The `DB_URL` already points to the Cloud SQL instance via
the Cloud SQL Java connector, no public IP is required.

## 3. Build and deploy

```bash
# Build the image, push it to Artifact Registry and deploy a new Cloud Run revision:
gcloud builds submit --config deploy/gcp/cloudbuild.yaml

# First deployment only - apply the full service definition (env vars, secrets, probes):
gcloud run services replace deploy/gcp/ncrm-backend-service.yaml --region europe-west1

# Allow public access to the API (skip if you front it with a load balancer/IAP):
gcloud run services add-iam-policy-binding ncrm-backend --region europe-west1 \
  --member allUsers --role roles/run.invoker
```

Subsequent releases only need `gcloud builds submit ...` – the Cloud Build pipeline
deploys the freshly built image to the existing service. You can also connect the
repository to Cloud Build triggers so every push to `main` deploys automatically.

## Verification

```bash
URL=$(gcloud run services describe ncrm-backend --region europe-west1 --format 'value(status.url)')
curl -fs "$URL/actuator/health"   # {"status":"UP"}
gcloud run services logs read ncrm-backend --region europe-west1   # application logs
```

## Alternative: GKE

If you prefer Kubernetes on Google Cloud, create a GKE Autopilot cluster and apply the
existing manifest `deploy/kubernetes/ncrm-backend.yaml` – only point the image to
Artifact Registry (`REGION-docker.pkg.dev/PROJECT_ID/ncrm/ncrm-backend`) and adjust the
Ingress to a GKE ingress class or a Google-managed certificate.
