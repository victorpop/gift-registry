package com.giftregistry.ui.store.list

import android.content.Context
import androidx.annotation.DrawableRes
import com.giftregistry.R

/**
 * Resolves a store's `logoAsset` string (from Firestore) to a bundled drawable
 * resource id. Falls back to `R.drawable.store_generic` when the named drawable
 * is not found.
 *
 * See proguard-rules.pro: `-keep class **.R$drawable { *; }` — required
 * so R8 does not strip drawables only referenced by runtime string lookup.
 */
@DrawableRes
fun resolveStoreLogoResId(context: Context, logoAsset: String): Int {
    val resId = context.resources.getIdentifier(
        logoAsset,
        "drawable",
        context.packageName,
    )
    return if (resId != 0) resId else R.drawable.store_generic
}
