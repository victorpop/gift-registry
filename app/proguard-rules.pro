# Gift Registry ProGuard Rules
# Add project-specific rules here.

# Phase 7 (STORE-01): Store logos are loaded at runtime via
# context.resources.getIdentifier(logoAsset, "drawable", packageName).
# R8 resource shrinking otherwise considers these drawables "unused" because
# there is no direct R.drawable.store_* reference in compiled code and
# removes them from the release APK. Keep the R$drawable inner class so
# identifier resolution continues to find them.
-keep class **.R$drawable { *; }
