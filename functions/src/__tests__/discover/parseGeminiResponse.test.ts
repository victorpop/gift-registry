/**
 * Phase 17 D-30/D-48: unit tests for `parseGeminiResponse` defensive parser.
 * RED-phase: committed before implementation; tests MUST fail.
 */
import { parseGeminiResponse } from "../../discover/parseGeminiResponse";

describe("parseGeminiResponse", () => {
  it("parses a valid JSON array", () => {
    const raw = JSON.stringify([
      {
        title: "T1",
        description: "d",
        image_url: "u",
        price: 10,
        currency: "RON",
        retailer_url: "https://emag.ro/x",
        retailer_name: "eMAG",
      },
    ]);
    const out = parseGeminiResponse(raw);
    expect(out).toHaveLength(1);
    expect(out[0].title).toBe("T1");
    expect(out[0].price).toBe(10);
  });

  it("strips ```json``` fences before parsing", () => {
    const raw =
      "```json\n[" +
      JSON.stringify({
        title: "T",
        price: 5,
        retailer_url: "https://x",
        currency: "RON",
        description: "",
        image_url: "",
        retailer_name: "",
      }) +
      "]\n```";
    expect(parseGeminiResponse(raw)).toHaveLength(1);
  });

  it("strips plain ``` fences before parsing", () => {
    const raw =
      "```\n[" +
      JSON.stringify({
        title: "T",
        price: 5,
        retailer_url: "https://x",
        currency: "RON",
        description: "",
        image_url: "",
        retailer_name: "",
      }) +
      "]\n```";
    expect(parseGeminiResponse(raw)).toHaveLength(1);
  });

  it("returns [] on malformed JSON and logs console.error", () => {
    const spy = jest.spyOn(console, "error").mockImplementation(() => {});
    expect(parseGeminiResponse("not json")).toEqual([]);
    expect(spy).toHaveBeenCalled();
    spy.mockRestore();
  });

  it("drops items missing title/price/retailer_url, keeps valid ones", () => {
    const raw = JSON.stringify([
      { title: "ok", price: 1, retailer_url: "u" },
      { title: "no-price", retailer_url: "u" }, // dropped
      { price: 1, retailer_url: "u" }, // dropped — no title
      { title: "no-url", price: 1 }, // dropped — no retailer_url
    ]);
    const out = parseGeminiResponse(raw);
    expect(out).toHaveLength(1);
    expect(out[0].title).toBe("ok");
  });

  it("coerces price as string '199.99' to 199.99 number", () => {
    const raw = JSON.stringify([{ title: "x", price: "199.99", retailer_url: "u" }]);
    expect(parseGeminiResponse(raw)[0]?.price).toBe(199.99);
  });

  it("drops item when price string is not parseable", () => {
    const raw = JSON.stringify([{ title: "x", price: "free", retailer_url: "u" }]);
    expect(parseGeminiResponse(raw)).toHaveLength(0);
  });

  it("truncates description over 200 chars to exactly 200", () => {
    const longDesc = "a".repeat(250);
    const raw = JSON.stringify([
      { title: "x", description: longDesc, price: 1, retailer_url: "u" },
    ]);
    expect(parseGeminiResponse(raw)[0]?.description).toHaveLength(200);
  });

  it("defaults image_url to '' when missing", () => {
    const raw = JSON.stringify([{ title: "x", price: 1, retailer_url: "u" }]);
    expect(parseGeminiResponse(raw)[0]?.image_url).toBe("");
  });

  it("defaults currency to 'RON' when missing", () => {
    const raw = JSON.stringify([{ title: "x", price: 1, retailer_url: "u" }]);
    expect(parseGeminiResponse(raw)[0]?.currency).toBe("RON");
  });

  it("returns [] when root is not an array", () => {
    const spy = jest.spyOn(console, "error").mockImplementation(() => {});
    expect(parseGeminiResponse(JSON.stringify({ products: [] }))).toEqual([]);
    spy.mockRestore();
  });

  it("returns [] when root is null", () => {
    const spy = jest.spyOn(console, "error").mockImplementation(() => {});
    expect(parseGeminiResponse("null")).toEqual([]);
    spy.mockRestore();
  });

  it("does not throw on null items inside the array (skips them)", () => {
    const raw = JSON.stringify([null, { title: "ok", price: 1, retailer_url: "u" }]);
    expect(parseGeminiResponse(raw)).toHaveLength(1);
  });
});
