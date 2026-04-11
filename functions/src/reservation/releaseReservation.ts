import { onTaskDispatched } from "firebase-functions/v2/tasks";

interface ReleasePayload { reservationId: string; }

export const releaseReservation = onTaskDispatched<ReleasePayload>(
  {
    region: "europe-west3",
    retryConfig: { maxAttempts: 3, minBackoffSeconds: 10 },
    rateLimits: { maxConcurrentDispatches: 20 },
    minInstances: 0,
    timeoutSeconds: 60,
  },
  async (_req) => {
    throw new Error("NOT_IMPLEMENTED: Wave 1 will implement this (Plan 02).");
  }
);
