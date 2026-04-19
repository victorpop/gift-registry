export default function SkeletonCard() {
  return (
    <article
      aria-hidden="true"
      className="flex gap-4 p-4 rounded-lg bg-surface-variant border border-outline"
    >
      <div className="w-24 h-24 rounded-md bg-outline animate-pulse flex-shrink-0" />
      <div className="flex-1 flex flex-col gap-2 justify-center">
        <div className="h-4 w-3/4 rounded bg-outline animate-pulse" />
        <div className="h-4 w-1/4 rounded bg-outline animate-pulse" />
      </div>
    </article>
  )
}
