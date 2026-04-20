/**
 * Tests for publicUrls config module.
 * Verifies that default values and URL builders produce the expected output
 * when running outside a deployed Functions context (where defineString returns its default).
 */

import { publicWebBaseUrl, buildRegistryUrl, buildReReserveUrl } from "../config/publicUrls";

describe("publicWebBaseUrl", () => {
  it("returns the default gift-registry-ro.web.app domain", () => {
    const url = publicWebBaseUrl();
    expect(url).toContain("gift-registry-ro.web.app");
  });

  it("does not have a trailing slash", () => {
    const url = publicWebBaseUrl();
    expect(url).not.toMatch(/\/$/);
  });
});

describe("buildRegistryUrl", () => {
  it("returns the correct URL for a given registry ID", () => {
    expect(buildRegistryUrl("abc")).toBe("https://gift-registry-ro.web.app/registry/abc");
  });

  it("does not produce a double slash when base has no trailing slash", () => {
    const url = buildRegistryUrl("abc");
    expect(url).not.toContain("//registry");
  });
});

describe("buildReReserveUrl", () => {
  it("returns the correct URL for a given reservation ID", () => {
    expect(buildReReserveUrl("r1")).toBe(
      "https://gift-registry-ro.web.app/reservation/r1/re-reserve"
    );
  });
});
