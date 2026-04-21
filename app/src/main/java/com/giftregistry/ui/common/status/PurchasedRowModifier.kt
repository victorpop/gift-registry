package com.giftregistry.ui.common.status

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/**
 * STAT-04 row-level opacity. Purchased items remain visible (handoff trust
 * pattern — prevents duplicate purchases) but are de-emphasised.
 */
const val PURCHASED_ROW_ALPHA: Float = 0.55f

/**
 * STAT-04: apply row-level alpha to a purchased item row.
 *
 * This modifier handles ONLY the row-level opacity. The caller is responsible
 * for the element-level treatments that cannot be expressed via Modifier alone:
 *
 * 1. Image grayscale + tint:
 *    ```
 *    AsyncImage(
 *      colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
 *    )
 *    Box(Modifier.matchParentSize().background(colors.ink.copy(alpha = 0.4f))) {
 *      Icon(Icons.Default.Check, tint = colors.paper, modifier = Modifier.size(20.dp))
 *    }
 *    ```
 * 2. Title strikethrough:
 *    ```
 *    Text(text = item.title, textDecoration = TextDecoration.LineThrough)
 *    ```
 *
 * Plan 11 (Registry detail redesign) consumes this at the item-row level.
 */
fun Modifier.purchasedVisualTreatment(): Modifier = this.alpha(PURCHASED_ROW_ALPHA)
