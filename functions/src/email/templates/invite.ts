/**
 * Invite email template — sent when a registry owner invites a user (REG-06/REG-07).
 * D-05: writes to Firestore mail collection via sendEmail helper.
 */

import { renderShell } from "./_shell";

export interface InviteVars {
  ownerName: string;
  registryName: string;
  registryUrl: string;
}

export function inviteTemplate(
  vars: InviteVars,
  locale: "en" | "ro"
): { subject: string; html: string; text: string } {
  const { ownerName, registryName, registryUrl } = vars;

  if (locale === "ro") {
    const subject = `Ai fost invitat la lista lui/ei ${ownerName}`;
    const text =
      `${ownerName} te-a invitat să vezi lista lor de cadouri: ${registryName}. ` +
      `Deschide-o pentru a vedea ce cadouri sunt disponibile. ${registryUrl}`;
    const html = renderShell({
      heading: "Ești invitat!",
      bodyHtml: `<p>${ownerName} te-a invitat să vezi lista lor de cadouri: <strong>${registryName}</strong>. Deschide-o pentru a vedea ce cadouri sunt disponibile.</p>`,
      ctaLabel: "Vezi lista",
      ctaUrl: registryUrl,
      footerText: "Ai primit acest email deoarece ai folosit Gift Registry.",
      preheader: `${ownerName} te-a invitat la lista lor de cadouri`,
    });
    return { subject, html, text };
  }

  // Default: en
  const subject = `You've been invited to ${ownerName}'s registry`;
  const text =
    `${ownerName} invited you to view their gift registry: ${registryName}. ` +
    `Open it to see what gifts are still available. ${registryUrl}`;
  const html = renderShell({
    heading: "You're invited!",
    bodyHtml: `<p>${ownerName} invited you to view their gift registry: <strong>${registryName}</strong>. Open it to see what gifts are still available.</p>`,
    ctaLabel: "View registry",
    ctaUrl: registryUrl,
    footerText: "You received this email because you used Gift Registry.",
    preheader: `${ownerName} invited you to their gift registry`,
  });
  return { subject, html, text };
}
