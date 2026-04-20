import { defineString } from "firebase-functions/params";

const DEFAULT_BASE_URL = "https://gift-registry-ro.web.app";

// Single source of truth for the public web base URL (the Firebase Hosting
// site for the React web fallback). Override at deploy time by setting
// PUBLIC_WEB_BASE_URL in the Functions environment; defaults to the
// Firebase-assigned hosting domain for the `gift-registry-ro` project.
const PUBLIC_WEB_BASE_URL = defineString("PUBLIC_WEB_BASE_URL", {
  default: DEFAULT_BASE_URL,
});

/**
 * Returns the public web base URL with any trailing slash stripped.
 * Falls back to the hardcoded default when called outside a deployed Functions
 * context (unit tests, emulator) where defineString returns an empty string.
 */
export function publicWebBaseUrl(): string {
  const configured = PUBLIC_WEB_BASE_URL.value();
  const base = configured || DEFAULT_BASE_URL;
  return base.replace(/\/$/, "");
}

/** Builds the public URL for a registry page, e.g. `${base}/registry/{id}`. */
export function buildRegistryUrl(registryId: string): string {
  return `${publicWebBaseUrl()}/registry/${registryId}`;
}

/** Builds the public URL for a re-reserve page, e.g. `${base}/reservation/{id}/re-reserve`. */
export function buildReReserveUrl(reservationId: string): string {
  return `${publicWebBaseUrl()}/reservation/${reservationId}/re-reserve`;
}
