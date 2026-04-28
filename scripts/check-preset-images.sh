#!/usr/bin/env bash
# Validate Phase 12 cover-photo preset drawables against the spec from
# 12-CONTEXT.md D-06: 1280x720 (16:9), JPEG q~85, ~150-300 KB target.
#
# Usage: ./scripts/check-preset-images.sh
# Exits 0 if all images pass, 1 if any issues found.

set -euo pipefail

DIR="app/src/main/res/drawable-xxhdpi"
TARGET_W=1280
TARGET_H=720
TARGET_RATIO="1.7778"   # 16:9
RATIO_TOLERANCE="0.01"  # ±1%
MAX_SIZE_KB=500         # flag anything bigger than this
MIN_SIZE_KB=20          # flag anything smaller (likely a placeholder)

if ! command -v sips >/dev/null 2>&1; then
  echo "ERROR: 'sips' not found. This script requires macOS." >&2
  exit 2
fi

cd "$(dirname "$0")/.."

if [[ ! -d "$DIR" ]]; then
  echo "ERROR: $DIR not found" >&2
  exit 2
fi

shopt -s nullglob
files=("$DIR"/preset_*.jpg)

if [[ ${#files[@]} -eq 0 ]]; then
  echo "ERROR: no preset_*.jpg files in $DIR" >&2
  exit 2
fi

oversized=()
wrong_ratio=()
wrong_size=()
placeholders=()
heavy=()
problems=0

printf "%-40s %10s %12s %10s %10s\n" "FILE" "DIMS" "RATIO" "SIZE" "STATUS"
printf "%-40s %10s %12s %10s %10s\n" "----" "----" "-----" "----" "------"

for f in "${files[@]}"; do
  name=$(basename "$f")
  w=$(sips -g pixelWidth "$f" 2>/dev/null | awk '/pixelWidth/ {print $2}')
  h=$(sips -g pixelHeight "$f" 2>/dev/null | awk '/pixelHeight/ {print $2}')
  fmt=$(sips -g format "$f" 2>/dev/null | awk '/format:/ {print $2}')
  bytes=$(stat -f%z "$f")
  size_kb=$((bytes / 1024))

  if [[ -z "$w" || -z "$h" ]]; then
    printf "%-40s %10s %12s %10s %10s\n" "$name" "?" "?" "${size_kb}K" "UNREADABLE"
    problems=$((problems+1))
    continue
  fi

  ratio=$(awk -v w="$w" -v h="$h" 'BEGIN { printf "%.4f", w/h }')
  ratio_diff=$(awk -v r="$ratio" -v t="$TARGET_RATIO" 'BEGIN { d = r - t; if (d < 0) d = -d; printf "%.4f", d }')

  status="OK"
  flag_problem=0

  if [[ "$fmt" != "jpeg" ]]; then
    status="NOT-JPEG"
    flag_problem=1
  elif (( $(awk -v d="$ratio_diff" -v t="$RATIO_TOLERANCE" 'BEGIN { print (d > t) }') )); then
    status="WRONG-RATIO"
    wrong_ratio+=("$name (${w}x${h}, ratio=$ratio)")
    flag_problem=1
  elif [[ $w -gt $((TARGET_W * 2)) || $h -gt $((TARGET_H * 2)) ]]; then
    status="TOO-LARGE"
    oversized+=("$name (${w}x${h})")
    flag_problem=1
  elif [[ $w -lt $TARGET_W || $h -lt $TARGET_H ]]; then
    status="TOO-SMALL"
    wrong_size+=("$name (${w}x${h})")
    flag_problem=1
  fi

  if [[ $size_kb -gt $MAX_SIZE_KB ]]; then
    if [[ "$status" == "OK" ]]; then status="HEAVY"; fi
    heavy+=("$name (${size_kb} KB)")
    flag_problem=1
  elif [[ $size_kb -lt $MIN_SIZE_KB ]]; then
    if [[ "$status" == "OK" ]]; then status="PLACEHOLDER"; fi
    placeholders+=("$name (${size_kb} KB)")
  fi

  if [[ $flag_problem -eq 1 ]]; then
    problems=$((problems+1))
  fi

  printf "%-40s %10s %12s %10s %10s\n" "$name" "${w}x${h}" "$ratio" "${size_kb}K" "$status"
done

echo
echo "Summary: ${#files[@]} files checked"
echo

if [[ ${#oversized[@]} -gt 0 ]]; then
  echo "OVERSIZED (>2x target ${TARGET_W}x${TARGET_H}):"
  printf "  - %s\n" "${oversized[@]}"
fi

if [[ ${#wrong_size[@]} -gt 0 ]]; then
  echo "TOO SMALL (<${TARGET_W}x${TARGET_H}):"
  printf "  - %s\n" "${wrong_size[@]}"
fi

if [[ ${#wrong_ratio[@]} -gt 0 ]]; then
  echo "WRONG RATIO (not 16:9):"
  printf "  - %s\n" "${wrong_ratio[@]}"
fi

if [[ ${#heavy[@]} -gt 0 ]]; then
  echo "HEAVY (>${MAX_SIZE_KB} KB — consider re-encoding at q=85):"
  printf "  - %s\n" "${heavy[@]}"
fi

if [[ ${#placeholders[@]} -gt 0 ]]; then
  echo "PLACEHOLDERS (<${MIN_SIZE_KB} KB — Pillow stubs from Plan 12-02, not curated):"
  printf "  - %s\n" "${placeholders[@]}"
fi

echo
if [[ $problems -eq 0 ]]; then
  echo "PASS: all images conform to spec"
  exit 0
else
  echo "FAIL: $problems file(s) need attention"
  exit 1
fi
