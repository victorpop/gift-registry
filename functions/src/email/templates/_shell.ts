/**
 * Shared email shell template for all Phase 6 email templates.
 * UI-SPEC "Common Email Shell" — 600px centered content card.
 * Inline styles only (no <style> block) for email client compatibility.
 */

export interface ShellOpts {
  heading: string;
  bodyHtml: string;
  ctaLabel: string;
  ctaUrl: string;
  footerText: string;
  preheader: string;
}

export function renderShell(opts: ShellOpts): string {
  const { heading, bodyHtml, ctaLabel, ctaUrl, footerText, preheader } = opts;
  return `<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${heading}</title></head>
<body style="margin:0; padding:0; background-color:#FFFBFE;">
<span style="display:none; visibility:hidden; mso-hide:all; max-height:0; max-width:0; overflow:hidden;">${preheader}</span>
<table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#FFFBFE;"><tr><td align="center">
  <table width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%;">
    <tr><td bgcolor="#6750A4" align="center" style="padding:20px 0; height:64px;">
      <span style="font-family:Arial,Helvetica,sans-serif; font-size:20px; font-weight:bold; color:#FFFFFF;">Gift Registry</span>
    </td></tr>
    <tr><td bgcolor="#FFFFFF" style="padding:32px;">
      <h1 style="margin:0 0 16px 0; font-family:Georgia,'Times New Roman',serif; font-size:24px; font-weight:bold; color:#1C1B1F; line-height:1.2;">${heading}</h1>
      <div style="font-family:Arial,Helvetica,sans-serif; font-size:16px; font-weight:normal; color:#1C1B1F; line-height:1.6;">${bodyHtml}</div>
      <table cellpadding="0" cellspacing="0" border="0" style="margin:24px auto;">
        <tr><td align="center" bgcolor="#6750A4" style="border-radius:6px; padding:0;">
          <a href="${ctaUrl}" style="display:inline-block; padding:14px 32px; font-family:Arial,Helvetica,sans-serif; font-size:16px; font-weight:bold; color:#FFFFFF; text-decoration:none; border-radius:6px;">${ctaLabel}</a>
        </td></tr>
      </table>
    </td></tr>
    <tr><td style="padding:16px 32px;">
      <hr style="border:none; border-top:1px solid #CAC4D0; margin:0 0 16px 0;">
      <p style="margin:0; font-family:Arial,Helvetica,sans-serif; font-size:12px; color:#49454F; text-align:center; line-height:1.5;">${footerText}</p>
    </td></tr>
  </table>
</td></tr></table></body></html>`;
}
