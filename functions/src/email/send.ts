/**
 * sendEmail — writes a mail document to the Firestore `mail` collection.
 * The Firebase Trigger Email extension picks up docs from this collection
 * and delivers them via configured SMTP (D-05, D-06).
 *
 * In the emulator (FUNCTIONS_EMULATOR=true), devMailLogger logs rendered
 * subject/text to console instead (D-08).
 */

import * as admin from "firebase-admin";

export interface SendEmailParams {
  to: string;
  subject: string;
  html: string;
  text: string;
}

export async function sendEmail(params: SendEmailParams): Promise<void> {
  if (!params.to || !params.subject) {
    throw new Error("sendEmail: 'to' and 'subject' are required");
  }
  const db = admin.firestore();
  await db.collection("mail").add({
    to: params.to,
    message: {
      subject: params.subject,
      html: params.html,
      text: params.text,
    },
  });
}
