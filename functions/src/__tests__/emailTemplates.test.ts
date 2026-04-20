/**
 * Tests for email template functions (expiry, invite, purchase) — en/ro variants.
 * Task 1, Plan 06-01.
 */

import { expiryTemplate } from "../email/templates/expiry";
import { inviteTemplate } from "../email/templates/invite";
import { purchaseTemplate } from "../email/templates/purchase";

describe("expiryTemplate", () => {
  const vars = {
    itemName: "Coffee Machine",
    registryName: "Wedding Registry",
    reReserveUrl: "https://gift-registry-ro.web.app/reservation/res1/re-reserve",
  };

  it("en: returns non-empty subject, html, text", () => {
    const result = expiryTemplate(vars, "en");
    expect(result.subject).toBeTruthy();
    expect(result.html).toBeTruthy();
    expect(result.text).toBeTruthy();
  });

  it("en: subject starts with 'Your reservation for'", () => {
    const result = expiryTemplate(vars, "en");
    expect(result.subject).toMatch(/^Your reservation for/);
  });

  it("en: subject includes itemName", () => {
    const result = expiryTemplate(vars, "en");
    expect(result.subject).toContain("Coffee Machine");
  });

  it("en: html contains CTA URL", () => {
    const result = expiryTemplate(vars, "en");
    expect(result.html).toContain(vars.reReserveUrl);
  });

  it("en: html contains 'Re-reserve this gift'", () => {
    const result = expiryTemplate(vars, "en");
    expect(result.html).toContain("Re-reserve this gift");
  });

  it("ro: subject starts with 'Rezervarea ta pentru'", () => {
    const result = expiryTemplate(vars, "ro");
    expect(result.subject).toMatch(/^Rezervarea ta pentru/);
  });

  it("ro: html contains 'Re-rezervă acest cadou'", () => {
    const result = expiryTemplate(vars, "ro");
    expect(result.html).toContain("Re-rezervă acest cadou");
  });

  it("ro: html contains CTA URL", () => {
    const result = expiryTemplate(vars, "ro");
    expect(result.html).toContain(vars.reReserveUrl);
  });
});

describe("inviteTemplate", () => {
  const vars = {
    ownerName: "Ana",
    registryName: "Baby Shower",
    registryUrl: "https://gift-registry-ro.web.app/registry/reg1",
  };

  it("en: returns non-empty subject, html, text", () => {
    const result = inviteTemplate(vars, "en");
    expect(result.subject).toBeTruthy();
    expect(result.html).toBeTruthy();
    expect(result.text).toBeTruthy();
  });

  it("en: subject contains 'invited'", () => {
    const result = inviteTemplate(vars, "en");
    expect(result.subject).toContain("invited");
  });

  it("en: html contains registry URL", () => {
    const result = inviteTemplate(vars, "en");
    expect(result.html).toContain(vars.registryUrl);
  });

  it("en: subject contains ownerName", () => {
    const result = inviteTemplate(vars, "en");
    expect(result.subject).toContain("Ana");
  });

  it("ro: subject contains 'invitat'", () => {
    const result = inviteTemplate(vars, "ro");
    expect(result.subject).toContain("invitat");
  });

  it("ro: html contains registry URL", () => {
    const result = inviteTemplate(vars, "ro");
    expect(result.html).toContain(vars.registryUrl);
  });
});

describe("purchaseTemplate", () => {
  const baseVars = {
    giverFirstName: "John",
    giverLastName: "Doe",
    itemName: "Blender",
    registryName: "Birthday Gifts",
    registryUrl: "https://gift-registry-ro.web.app/registry/reg2",
  };

  it("en: returns non-empty subject, html, text", () => {
    const result = purchaseTemplate(baseVars, "en");
    expect(result.subject).toBeTruthy();
    expect(result.html).toBeTruthy();
    expect(result.text).toBeTruthy();
  });

  it("en: subject is exact copy", () => {
    const result = purchaseTemplate(baseVars, "en");
    expect(result.subject).toBe("Someone bought a gift from your registry!");
  });

  it("en: html contains registry URL", () => {
    const result = purchaseTemplate(baseVars, "en");
    expect(result.html).toContain(baseVars.registryUrl);
  });

  it("ro: subject is exact copy", () => {
    const result = purchaseTemplate(baseVars, "ro");
    expect(result.subject).toBe("Cineva a cumpărat un cadou din lista ta!");
  });

  it("ro: html contains registry URL", () => {
    const result = purchaseTemplate(baseVars, "ro");
    expect(result.html).toContain(baseVars.registryUrl);
  });

  it("en: when giverFirstName is null, text contains 'Someone purchased' not 'null'", () => {
    const result = purchaseTemplate(
      { ...baseVars, giverFirstName: null, giverLastName: null },
      "en"
    );
    expect(result.text).toContain("Someone purchased");
    expect(result.text).not.toContain("null");
    expect(result.html).toContain("Someone purchased");
    expect(result.html).not.toContain("null null");
  });

  it("ro: when giverFirstName is null, text contains 'Cineva a cumpărat' not 'null'", () => {
    const result = purchaseTemplate(
      { ...baseVars, giverFirstName: null, giverLastName: null },
      "ro"
    );
    expect(result.text).toContain("Cineva a cumpărat");
    expect(result.text).not.toContain("null");
  });
});
