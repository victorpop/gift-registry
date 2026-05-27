/**
 * Phase 17 D-28: Romanian retailer category lists — verbatim from spec.
 *
 * Site-list selection at prompt-build time is by simple Romanian + English
 * keyword match on the user's query. Per CONTEXT.md "Claude's Discretion",
 * keyword sufficiency for v1 (no regex word-boundary matching needed).
 *
 * Mutating these lists requires a planning revision (the spec calls them
 * verbatim — drift becomes a contract violation against CONTEXT.md).
 */

export const RETAILERS = {
  universal: ["emag.ro", "altex.ro", "carrefour.ro", "vivre.eu", "elefant.ro", "flanco.ro"],
  birthday: [
    "mindblower.ro",
    "funfox.ro",
    "borealy.ro",
    "douglas.ro",
    "sephora.ro",
    "libris.ro",
    "carturesti.ro",
  ],
  wedding: ["23h.ro", "crisiashop.ro", "wedday.ro", "happycards.ro", "magazinulmireselor.ro"],
  housewarming: [
    "jysk.ro",
    "mobexpert.ro",
    "ikea.com/ro",
    "dedeman.ro",
    "leroymerlin.ro",
    "vivre.eu",
    "insignis.ro",
    "kika.ro",
    "somproduct.ro",
  ],
  baby_shower: [
    "bekid.ro",
    "babyneeds.ro",
    "bebelul.ro",
    "bebebliss.ro",
    "bebenou.ro",
    "chicco.ro",
    "erfi.ro",
    "babymatters.ro",
    "noriel.ro",
  ],
  christmas: [
    "borealy.ro",
    "mindblower.ro",
    "funfox.ro",
    "gourmetgift.ro",
    "douglas.ro",
    "sephora.ro",
    "kaufland.ro",
    "lidl.ro",
  ],
} as const;

export type OccasionCategory = keyof typeof RETAILERS;

/**
 * D-28: lowercase substring match against Romanian + English keywords.
 * When a category matches, returns `universal ⧺ category` (universal first
 * preserves the "highest priority first" instruction in the prompt).
 * Default (no match) = universal only.
 */
export function selectSitesForQuery(query: string): string[] {
  const q = query.toLowerCase();
  const matches = (...needles: string[]): boolean => needles.some((n) => q.includes(n));

  if (matches("ziua de naștere", "ziua de nastere", "birthday")) {
    return [...RETAILERS.universal, ...RETAILERS.birthday];
  }
  if (matches("nuntă", "nunta", "wedding")) {
    return [...RETAILERS.universal, ...RETAILERS.wedding];
  }
  if (matches("casă nouă", "casa noua", "warming", "mutare", "housewarming")) {
    return [...RETAILERS.universal, ...RETAILERS.housewarming];
  }
  if (matches("bebeluș", "bebelus", "baby", "shower")) {
    return [...RETAILERS.universal, ...RETAILERS.baby_shower];
  }
  if (matches("crăciun", "craciun", "christmas")) {
    return [...RETAILERS.universal, ...RETAILERS.christmas];
  }
  return [...RETAILERS.universal];
}
