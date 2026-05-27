/**
 * Phase 17 D-28/D-48: unit tests for `RETAILERS` catalog + `selectSitesForQuery`.
 * RED-phase: committed before implementation; tests MUST fail.
 */
import { RETAILERS, selectSitesForQuery } from "../../discover/retailers";

describe("RETAILERS catalog (D-28 verbatim from spec)", () => {
  it("exposes the 6 documented categories", () => {
    const keys = Object.keys(RETAILERS).sort();
    expect(keys).toEqual(
      ["baby_shower", "birthday", "christmas", "housewarming", "universal", "wedding"].sort(),
    );
  });

  it("universal contains the spec hosts", () => {
    expect(RETAILERS.universal).toEqual([
      "emag.ro",
      "altex.ro",
      "carrefour.ro",
      "vivre.eu",
      "elefant.ro",
      "flanco.ro",
    ]);
  });

  it("birthday contains mindblower.ro + carturesti.ro (spec verbatim)", () => {
    expect(RETAILERS.birthday).toContain("mindblower.ro");
    expect(RETAILERS.birthday).toContain("carturesti.ro");
    expect(RETAILERS.birthday).toContain("borealy.ro");
  });

  it("wedding contains magazinulmireselor.ro (spec verbatim)", () => {
    expect(RETAILERS.wedding).toContain("magazinulmireselor.ro");
    expect(RETAILERS.wedding).toContain("23h.ro");
  });

  it("housewarming contains ikea.com/ro (spec verbatim, with slash)", () => {
    expect(RETAILERS.housewarming).toContain("ikea.com/ro");
    expect(RETAILERS.housewarming).toContain("dedeman.ro");
  });

  it("baby_shower contains noriel.ro + chicco.ro (spec verbatim)", () => {
    expect(RETAILERS.baby_shower).toContain("noriel.ro");
    expect(RETAILERS.baby_shower).toContain("chicco.ro");
  });

  it("christmas contains lidl.ro + kaufland.ro (spec verbatim)", () => {
    expect(RETAILERS.christmas).toContain("lidl.ro");
    expect(RETAILERS.christmas).toContain("kaufland.ro");
  });
});

describe("selectSitesForQuery (D-28 heuristic)", () => {
  it("Romanian birthday phrase → universal + birthday", () => {
    const sites = selectSitesForQuery("cadou de ziua de naștere");
    expect(sites).toContain("emag.ro");
    expect(sites).toContain("mindblower.ro");
  });

  it("ASCII-fallback Romanian birthday phrase → universal + birthday", () => {
    const sites = selectSitesForQuery("cadou de ziua de nastere");
    expect(sites).toContain("mindblower.ro");
  });

  it("Romanian wedding phrase → universal + wedding", () => {
    const sites = selectSitesForQuery("listă de nuntă");
    expect(sites).toContain("magazinulmireselor.ro");
    expect(sites).toContain("emag.ro");
  });

  it("Romanian housewarming → universal + housewarming", () => {
    const sites = selectSitesForQuery("cadou casă nouă");
    expect(sites).toContain("ikea.com/ro");
    expect(sites).toContain("dedeman.ro");
  });

  it("Romanian baby_shower → universal + baby_shower", () => {
    const sites = selectSitesForQuery("cadou pentru bebeluș");
    expect(sites).toContain("noriel.ro");
  });

  it("Romanian christmas → universal + christmas", () => {
    const sites = selectSitesForQuery("cadou de crăciun");
    expect(sites).toContain("kaufland.ro");
    expect(sites).toContain("lidl.ro");
  });

  it("English equivalents map to the same categories", () => {
    expect(selectSitesForQuery("birthday gift")).toContain("mindblower.ro");
    expect(selectSitesForQuery("wedding gift")).toContain("magazinulmireselor.ro");
    expect(selectSitesForQuery("housewarming gift")).toContain("ikea.com/ro");
    expect(selectSitesForQuery("baby shower")).toContain("noriel.ro");
    expect(selectSitesForQuery("christmas gift")).toContain("kaufland.ro");
  });

  it("neutral query returns universal only", () => {
    const sites = selectSitesForQuery("cafetiera espresso");
    expect(sites).toEqual([...RETAILERS.universal]);
  });

  it("is case-insensitive for the keyword match", () => {
    const sites = selectSitesForQuery("BIRTHDAY GIFT");
    expect(sites).toContain("mindblower.ro");
  });
});
