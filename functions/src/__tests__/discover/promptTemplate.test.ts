/**
 * Phase 17 D-29/D-48: unit tests for `buildPrompt`.
 * RED-phase: committed before implementation; tests MUST fail.
 */
import { buildPrompt } from "../../discover/promptTemplate";

describe("buildPrompt (D-29 verbatim template)", () => {
  const sites = ["emag.ro", "altex.ro", "mindblower.ro"];

  it("returns the user query verbatim in userPrompt", () => {
    const { userPrompt } = buildPrompt("espresso machine", sites);
    expect(userPrompt).toBe("espresso machine");
  });

  it("preserves diacritics in userPrompt", () => {
    const { userPrompt } = buildPrompt("cadou pentru bunică", sites);
    expect(userPrompt).toBe("cadou pentru bunică");
  });

  it("systemPrompt contains the verbatim 'Return ONLY a strict JSON array' directive", () => {
    const { systemPrompt } = buildPrompt("anything", sites);
    expect(systemPrompt).toContain("Return ONLY a strict JSON array");
  });

  it("systemPrompt embeds every supplied site in priority order", () => {
    const { systemPrompt } = buildPrompt("anything", sites);
    expect(systemPrompt).toContain("emag.ro");
    expect(systemPrompt).toContain("altex.ro");
    expect(systemPrompt).toContain("mindblower.ro");
    // Sites appear in the order passed in (priority order).
    const idxEmag = systemPrompt.indexOf("emag.ro");
    const idxAltex = systemPrompt.indexOf("altex.ro");
    const idxMind = systemPrompt.indexOf("mindblower.ro");
    expect(idxEmag).toBeLessThan(idxAltex);
    expect(idxAltex).toBeLessThan(idxMind);
  });

  it("systemPrompt documents the per-item schema", () => {
    const { systemPrompt } = buildPrompt("anything", sites);
    expect(systemPrompt).toContain("Schema per item:");
    expect(systemPrompt).toContain("title");
    expect(systemPrompt).toContain("description");
    expect(systemPrompt).toContain("image_url");
    expect(systemPrompt).toContain("price");
    expect(systemPrompt).toContain("currency");
    expect(systemPrompt).toContain("retailer_url");
    expect(systemPrompt).toContain("retailer_name");
  });

  it("systemPrompt requires RON currency", () => {
    const { systemPrompt } = buildPrompt("anything", sites);
    expect(systemPrompt).toContain("RON");
  });

  it("systemPrompt states the 5–15 item count range", () => {
    const { systemPrompt } = buildPrompt("anything", sites);
    expect(systemPrompt).toContain("Return between 5 and 15 items");
  });

  it("systemPrompt instructs to drop incomplete items", () => {
    const { systemPrompt } = buildPrompt("anything", sites);
    expect(systemPrompt).toContain("Drop items missing");
    expect(systemPrompt).toContain("retailer_url");
  });
});
