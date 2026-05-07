/**
 * Loading-state placeholder for a single item card (UI-SPEC Loading / Skeleton States).
 * Image area: paperDeep, aspect 4:3 mobile / 16:10 ≥ 640 px (matches real ItemCard).
 * Title bar: 60% width, 16 px height. Price bar: 40% width, 14 px height.
 * Quantity in grid: 6 (set by parent).
 */
export default function SkeletonCard() {
  return (
    <article
      aria-hidden="true"
      className="flex flex-col rounded-gm-card overflow-hidden border border-gm-line bg-gm-paper"
    >
      <div className="aspect-[4/3] sm:aspect-[16/10] bg-gm-paperDeep animate-pulse" />
      <div className="flex flex-col gap-3 p-4">
        <div className="h-4 w-3/5 rounded bg-gm-paperDeep animate-pulse" />
        <div className="flex justify-between">
          <div className="h-3 w-2/5 rounded bg-gm-paperDeep animate-pulse" />
          <div className="h-3 w-1/4 rounded bg-gm-paperDeep animate-pulse" />
        </div>
      </div>
    </article>
  )
}
