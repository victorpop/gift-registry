/**
 * Purchase notification email template — sent when a giver confirms purchase (NOTF-02).
 * D-05: writes to Firestore mail collection via sendEmail helper.
 */

import { renderShell } from "./_shell";

export interface PurchaseVars {
  giverFirstName: string | null;
  giverLastName: string | null;
  itemName: string;
  registryName: string;
  registryUrl: string;
}

export function purchaseTemplate(
  vars: PurchaseVars,
  locale: "en" | "ro"
): { subject: string; html: string; text: string } {
  const { giverFirstName, giverLastName, itemName, registryName, registryUrl } = vars;

  const hasGiverName = giverFirstName && giverFirstName.trim().length > 0;

  if (locale === "ro") {
    const subject = "Cineva a cumpărat un cadou din lista ta!";
    const giverDesc = hasGiverName
      ? `${giverFirstName} ${giverLastName}`
      : "Cineva";
    const bodyText = `${giverDesc} a cumpărat ${itemName} din ${registryName}.`;
    const text = `${bodyText} ${registryUrl}`;
    const html = renderShell({
      heading: "Un cadou a fost cumpărat",
      bodyHtml: `<p>${bodyText}</p>`,
      ctaLabel: "Vezi lista ta",
      ctaUrl: registryUrl,
      footerText: "Ai primit acest email deoarece ai folosit Gift Registry.",
      preheader: `${itemName} a fost cumpărat din lista ta`,
    });
    return { subject, html, text };
  }

  // Default: en
  const subject = "Someone bought a gift from your registry!";
  const giverDesc = hasGiverName
    ? `${giverFirstName} ${giverLastName}`
    : "Someone";
  const bodyText = hasGiverName
    ? `${giverDesc} purchased ${itemName} from ${registryName}.`
    : `Someone purchased ${itemName} from ${registryName}.`;
  const text = `${bodyText} ${registryUrl}`;
  const html = renderShell({
    heading: "A gift was purchased",
    bodyHtml: `<p>${bodyText}</p>`,
    ctaLabel: "View your registry",
    ctaUrl: registryUrl,
    footerText: "You received this email because you used Gift Registry.",
    preheader: `${itemName} was purchased from your registry`,
  });
  return { subject, html, text };
}
