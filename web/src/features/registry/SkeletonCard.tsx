/**
 * Loading-state placeholder for a single item card (k37 redesign — vertical stack:
 * image / shop / name / price).
 *
 * Image area: paperDeep, aspect 4:3 mobile / 16:10 ≥ 640 px (matches real ItemCard).
 * Stack (matches the new ItemCard body — gap-2, p-4):
 *   - Shop bar: small, mono-width (matches MonoCaption micro 10 px)
 *   - Name bar: wider, taller (matches h3 15 px)
 *   - Price bar: medium-bold (matches price 15 px font-semibold)
 * Quantity in grid: 6 (set by parent).
 */
export default function SkeletonCard() {
  return (
    <article
      aria-hidden="true"
      className="flex flex-col rounded-gm-card overflow-hidden border border-gm-line bg-gm-paper"
    >
      <div className="aspect-[4/3] sm:aspect-[16/10] bg-gm-paperDeep animate-pulse" />
      <div className="flex flex-col gap-2 p-4">
        {/* Shop line skeleton: small/short, mimics MonoCaption micro */}
        <div className="h-3 w-1/4 rounded bg-gm-paperDeep animate-pulse" />
        {/* Name skeleton: wider, slightly taller */}
        <div className="h-4 w-3/5 rounded bg-gm-paperDeep animate-pulse" />
        {/* Price skeleton: medium */}
        <div className="h-4 w-1/3 rounded bg-gm-paperDeep animate-pulse" />
      </div>
    </article>
  )
}
