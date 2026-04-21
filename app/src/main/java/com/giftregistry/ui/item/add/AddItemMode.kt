package com.giftregistry.ui.item.add

/**
 * SCR-10: 3-mode segmented control on the Add Item screen (Paste URL / Browse
 * stores / Manual).
 *
 * Default = PasteUrl per CONTEXT.md § Add Item URL. The default ordinal is
 * exposed as a top-level const val so `rememberSaveable { mutableIntStateOf(ADD_ITEM_MODE_DEFAULT_ORDINAL) }`
 * can initialise without importing the enum (Phase 10 precedent from STATE.md:
 * "Tab index uses Int via rememberSaveable mutableIntStateOf(0), not sealed class").
 *
 * Unit-tested by AddItemModeTest (Wave 0).
 */
enum class AddItemMode { PasteUrl, BrowseStores, Manual }

/** Default AddItemMode ordinal for rememberSaveable initial value. */
const val ADD_ITEM_MODE_DEFAULT_ORDINAL: Int = 0
