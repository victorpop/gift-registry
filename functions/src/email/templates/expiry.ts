/**
 * Expiry email template — sent when a reservation expires (NOTF-03).
 * D-05: writes to Firestore mail collection via sendEmail helper.
 */

import { renderShell } from "./_shell";

export interface ExpiryVars {
  itemName: string;
  registryName: string;
  reReserveUrl: string;
}

export function expiryTemplate(
  vars: ExpiryVars,
  locale: "en" | "ro"
): { subject: string; html: string; text: string } {
  const { itemName, registryName, reReserveUrl } = vars;

  if (locale === "ro") {
    const subject = `Rezervarea ta pentru "${itemName}" a expirat`;
    const text =
      `Rezervarea ta de 30 de minute pentru ${itemName} din ${registryName} a expirat. ` +
      `Articolul este acum disponibil pentru alți donatori. Re-rezervă: ${reReserveUrl}`;
    const html = renderShell({
      heading: "Rezervarea ta a expirat",
      bodyHtml: `<p>Rezervarea ta de 30 de minute pentru <strong>${itemName}</strong> din ${registryName} a expirat. Articolul este acum disponibil pentru alți donatori.</p>`,
      ctaLabel: "Re-rezervă acest cadou",
      ctaUrl: reReserveUrl,
      footerText: "Ai primit acest email deoarece ai folosit Gift Registry.",
      preheader: `Rezervarea ta pentru ${itemName} a expirat — re-rezervă acum`,
    });
    return { subject, html, text };
  }

  // Default: en
  const subject = `Your reservation for "${itemName}" has expired`;
  const text =
    `Your 30-minute reservation for ${itemName} from ${registryName} has expired. ` +
    `The item is now available for other givers. Re-reserve: ${reReserveUrl}`;
  const html = renderShell({
    heading: "Your reservation expired",
    bodyHtml: `<p>Your 30-minute reservation for <strong>${itemName}</strong> from ${registryName} has expired. The item is now available for other givers.</p>`,
    ctaLabel: "Re-reserve this gift",
    ctaUrl: reReserveUrl,
    footerText: "You received this email because you used Gift Registry.",
    preheader: `Your reservation for ${itemName} expired — re-reserve now`,
  });
  return { subject, html, text };
}
