/**
 * Dev-only mail logger (D-08).
 * Logs rendered mail docs to console when the Functions emulator is running.
 * Only exported from index.ts when process.env.FUNCTIONS_EMULATOR === "true",
 * so this function never runs in production.
 *
 * In production, the Firebase Trigger Email extension handles mail/ docs instead.
 */

import { onDocumentCreated } from "firebase-functions/v2/firestore";

export const devMailLogger = onDocumentCreated(
  { document: "mail/{docId}", region: "europe-west3" },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;
    const { to, message } = data as {
      to: string;
      message?: { subject?: string; text?: string; html?: string };
    };
    // eslint-disable-next-line no-console
    console.log(
      "\n[DEV MAIL] ------------------------------------------------------------\n" +
        `To: ${to}\n` +
        `Subject: ${message?.subject ?? "(no subject)"}\n` +
        `Text:\n${message?.text ?? "(empty)"}\n` +
        "[DEV MAIL] ------------------------------------------------------------\n"
    );
  }
);
