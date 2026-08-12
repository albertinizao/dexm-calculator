# Cloud Run + Firestore + IAP deployment

## Authentication modes

The application has only two authentication modes:

- `APP_AUTH_MODE=local` is the default for local development. It creates a
  stable administrator identity automatically; it does not use Google OAuth,
  HTTP sessions, CSRF cookies, client secrets, or a login flow.
- `APP_AUTH_MODE=iap` is required in Cloud Run. It validates the signed
  `X-Goog-IAP-JWT-Assertion` header on every request. Do not set
  `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, or `APP_SECURITY_FRONTEND_URL`.

Enable IAP on the Cloud Run service **before** deploying or switching to IAP
mode. IAP must be the only public authentication boundary; the application
still validates IAP's signed assertion as defence in depth.

Find the numeric project number (not the project ID):

```powershell
gcloud projects describe PROJECT_ID --format='value(projectNumber)'
```

For this service in Madrid, set the exact audience:

```text
APP_IAP_AUDIENCE=/projects/<PROJECT_NUMBER>/locations/europe-southwest1/services/dexm-calculator
```

The application fails closed at startup if this value is absent in IAP mode.

## IAP setup and runtime identity

Create Firestore in Native mode. Deploy Cloud Run with a service account that
has `roles/datastore.user`. Use Application Default Credentials supplied by
Cloud Run; never create or mount a service-account JSON key and never set
`GOOGLE_APPLICATION_CREDENTIALS`.

Enable IAP directly on the Cloud Run service. Its service agent must be allowed
to invoke the service:

```powershell
$projectNumber = gcloud projects describe PROJECT_ID --format='value(projectNumber)'
gcloud run services add-iam-policy-binding dexm-calculator `
  --region europe-southwest1 `
  --member="serviceAccount:service-$projectNumber@gcp-sa-iap.iam.gserviceaccount.com" `
  --role=roles/run.invoker
```

Grant users or groups the IAP-secured Web App User role through IAP, then
enable IAP (for example, `gcloud run deploy ... --no-allow-unauthenticated --iap`).

## Build and deploy

```powershell
docker build -t dexm-firestore .
$projectNumber = gcloud projects describe PROJECT_ID --format='value(projectNumber)'
$audience = "/projects/$projectNumber/locations/europe-southwest1/services/dexm-calculator"
gcloud run deploy dexm-calculator --image REGION-docker.pkg.dev/PROJECT/REPOSITORY/dexm-firestore `
  --region europe-southwest1 --service-account dexm-run@PROJECT.iam.gserviceaccount.com `
  --no-allow-unauthenticated --iap `
  --set-env-vars "APP_AUTH_MODE=iap,APP_IAP_AUDIENCE=$audience"
```

Cloud Run supplies `PORT`. The Docker image serves the Vue SPA and `/api` from
the same Spring Boot container.

## Local Firestore emulator

Start the emulator and run with `APP_AUTH_MODE=local`,
`SPRING_PROFILES_ACTIVE=firestore`, and `FIRESTORE_EMULATOR_HOST=localhost:8080`.
Set `GOOGLE_CLOUD_PROJECT` to any local project identifier such as `dexm-local`.
No OAuth or service-account JSON is required.
