/**
 * Tests for fetchOgMetadata Cloud Function (quick-260525-gqs).
 * Pins the meta-refresh follow contract + happy path + empty fallback.
 */

type MockFetchResponse = {
  ok: boolean;
  status: number;
  text: () => Promise<string>;
};

function mockFetchSequence(responses: Array<MockFetchResponse | Error>) {
  let i = 0;
  return jest.fn(async () => {
    const r = responses[i++];
    if (r instanceof Error) throw r;
    if (!r) throw new Error(`mockFetchSequence: ran out of responses after ${i - 1} calls`);
    return r as unknown as Response;
  });
}

function htmlWith(body: string): MockFetchResponse {
  return { ok: true, status: 200, text: async () => `<!DOCTYPE html><html><head>${body}</head><body></body></html>` };
}

function metaRefreshHtml(
  target: string,
  opts?: { quote?: "'" | "\"" | ""; key?: "url" | "URL"; sep?: ";" | "; " }
): MockFetchResponse {
  const q = opts?.quote ?? "'";
  const k = opts?.key ?? "url";
  const sep = opts?.sep ?? ";";
  // When the URL quote is a double-quote, the content attribute itself must use
  // single-quote delimiters to avoid producing malformed HTML (which node-html-parser
  // would truncate). Real-world pages that use double-quoted URLs in meta-refresh
  // do the same. For single- or no-quote variants, double-quote delimiters work fine.
  if (q === '"') {
    return htmlWith(`<meta http-equiv='refresh' content='0${sep}${k}=${q}${target}${q}' />`);
  }
  return htmlWith(`<meta http-equiv="refresh" content="0${sep}${k}=${q}${target}${q}" />`);
}

type OgMetadataResponse = {
  title: string | null;
  imageUrl: string | null;
  price: string | null;
  priceAmount: string | null;
  priceCurrency: string | null;
  siteName: string | null;
};

const EMPTY: OgMetadataResponse = {
  title: null, imageUrl: null, price: null, priceAmount: null, priceCurrency: null, siteName: null,
};

// Use the exported handler directly for testability.
import { fetchOgMetadataHandler } from "../registry/fetchOgMetadata";

const callFn = (url: string): Promise<OgMetadataResponse> =>
  fetchOgMetadataHandler({ data: { url } }) as Promise<OgMetadataResponse>;

describe("fetchOgMetadata — meta-refresh + happy path + empty fallback", () => {
  let fetchSpy: jest.SpyInstance;
  let warnSpy: jest.SpyInstance;
  let errorSpy: jest.SpyInstance;
  let logSpy: jest.SpyInstance;

  beforeEach(() => {
    warnSpy = jest.spyOn(console, "warn").mockImplementation(() => {});
    errorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
    logSpy = jest.spyOn(console, "log").mockImplementation(() => {});
  });
  afterEach(() => {
    fetchSpy?.mockRestore();
    warnSpy.mockRestore();
    errorSpy.mockRestore();
    logSpy.mockRestore();
  });

  it("happy path: returns og:title + og:image when present on first response", async () => {
    fetchSpy = jest.spyOn(global, "fetch").mockImplementation(
      mockFetchSequence([
        htmlWith(
          `<meta property="og:title" content="X"><meta property="og:image" content="https://cdn/img.jpg">`
        ),
      ])
    );

    const result = await callFn("https://www.emag.ro/product/pd/ABC/");

    expect(result.title).toBe("X");
    expect(result.imageUrl).toBe("https://cdn/img.jpg");
    expect(fetchSpy).toHaveBeenCalledTimes(1);
  });

  it("meta-refresh: follows one hop and parses OG tags from destination", async () => {
    fetchSpy = jest.spyOn(global, "fetch").mockImplementation(
      mockFetchSequence([
        metaRefreshHtml("/canonical/pd/X/"),
        htmlWith(
          `<meta property="og:title" content="Telefon iPhone 17 Pro Max - eMAG.ro"><meta property="og:image" content="https://cdn/img.jpg">`
        ),
      ])
    );

    const result = await callFn("https://www.emag.ro/stale/pd/X/");

    expect(result.title).toBe("Telefon iPhone 17 Pro Max - eMAG.ro");
    expect(result.imageUrl).toBe("https://cdn/img.jpg");
    expect(fetchSpy).toHaveBeenCalledTimes(2);
    const secondCallUrl = fetchSpy.mock.calls[1][0] as string;
    expect(secondCallUrl).toBe("https://www.emag.ro/canonical/pd/X/");
  });

  it.each([
    ["space no quotes", "/dest/", { sep: "; " as "; ", quote: "" as "", key: "url" as "url" }],
    ["uppercase URL key with single quotes", "/dest/", { sep: ";" as ";", quote: "'" as "'", key: "URL" as "URL" }],
    ["double quotes", "/dest/", { sep: ";" as ";", quote: "\"" as "\"", key: "url" as "url" }],
  ])(
    "meta-refresh: handles content variant — %s",
    async (_label, target, opts) => {
      fetchSpy = jest.spyOn(global, "fetch").mockImplementation(
        mockFetchSequence([
          metaRefreshHtml(target, opts),
          htmlWith(
            `<meta property="og:title" content="Variant Title"><meta property="og:image" content="https://cdn/variant.jpg">`
          ),
        ])
      );

      const result = await callFn("https://www.emag.ro/source/pd/Y/");

      expect(result.title).toBe("Variant Title");
      expect(fetchSpy).toHaveBeenCalledTimes(2);
    }
  );

  it("meta-refresh: max 3 fetches total, then returns empty", async () => {
    fetchSpy = jest.spyOn(global, "fetch").mockImplementation(
      mockFetchSequence([
        metaRefreshHtml("/hop1/pd/X/"),
        metaRefreshHtml("/hop2/pd/X/"),
        metaRefreshHtml("/hop3/pd/X/"),
        // 4th response should NEVER be called
        htmlWith(`<meta property="og:title" content="Should not reach">`),
      ])
    );

    const result = await callFn("https://www.emag.ro/start/pd/X/");

    expect(fetchSpy).toHaveBeenCalledTimes(3);
    expect(result).toEqual(EMPTY);
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining("max hops")
    );
  });

  it("meta-refresh: cross-origin redirect is NOT followed", async () => {
    fetchSpy = jest.spyOn(global, "fetch").mockImplementation(
      mockFetchSequence([
        htmlWith(`<meta http-equiv="refresh" content="0;url='https://evil.com/page'" />`),
        // 2nd response should NEVER be called
        htmlWith(`<meta property="og:title" content="Evil page">`),
      ])
    );

    const result = await callFn("https://www.emag.ro/x/pd/X/");

    expect(fetchSpy).toHaveBeenCalledTimes(1);
    expect(result).toEqual(EMPTY);
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining("cross-origin")
    );
  });

  it("non-meta-refresh empty page: returns empty (no regression)", async () => {
    fetchSpy = jest.spyOn(global, "fetch").mockImplementation(
      mockFetchSequence([
        htmlWith("<title>Some Page</title>"), // no OG tags, no meta-refresh
      ])
    );

    const result = await callFn("https://www.emag.ro/noogs/pd/Z/");

    // title might be populated from <title> tag; all OG-based fields should be null
    expect(result.imageUrl).toBeNull();
    expect(result.price).toBeNull();
    expect(fetchSpy).toHaveBeenCalledTimes(1);
  });

  it("fetch throws (network error): returns empty shape, does not crash", async () => {
    fetchSpy = jest.spyOn(global, "fetch").mockImplementation(
      mockFetchSequence([new Error("network down")])
    );

    const result = await callFn("https://www.emag.ro/fail/pd/F/");

    expect(result).toEqual(EMPTY);
    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("network down")
    );
  });
});
