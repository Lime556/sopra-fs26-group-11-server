#!/usr/bin/env bash
set -euo pipefail

# Usage: ./deploy-cloud-run.sh PROJECT_ID [REGION] [SERVICE] [IMAGE_TAG]
# Example: ./deploy-cloud-run.sh my-project europe-west1 soprafs26-server latest

PROJECT_ID=${1:?"Missing PROJECT_ID. Usage: $0 PROJECT_ID [REGION] [SERVICE] [IMAGE_TAG]"}
REGION=${2:-europe-west1}
SERVICE=${3:-soprafs26-server}
IMAGE_TAG=${4:-latest}
IMAGE=gcr.io/${PROJECT_ID}/sopra-fs26-server:${IMAGE_TAG}

# Build and push image using Cloud Build (recommended)
printf "Building and pushing image %s via Cloud Build...\n" "${IMAGE}"
gcloud builds submit --tag "${IMAGE}"

# Deploy to Cloud Run
printf "Deploying to Cloud Run service '%s' in region %s...\n" "${SERVICE}" "${REGION}"
gcloud run deploy "${SERVICE}" \
  --image "${IMAGE}" \
  --platform managed \
  --region "${REGION}" \
  --allow-unauthenticated \
  --set-env-vars SPRING_PROFILES_ACTIVE=production

printf "Deployment complete. Service URL:\n"
gcloud run services describe "${SERVICE}" --platform managed --region "${REGION}" --format 'value(status.url)'
