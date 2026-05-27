package com.giftregistry.ui.item.add

/**
 * SCR-10: 2-mode segmented control on the Add Item screen (Paste URL / Manual).
 *
 * Default = PasteUrl per CONTEXT.md § Add Item URL. The default ordinal is
 * exposed as a top-level const val so `rememberSaveable { mutableIntStateOf(ADD_ITEM_MODE_DEFAULT_ORDINAL) }`
 * can initialise without importing the enum (Phase 10 precedent from STATE.md:
 * "Tab index uses Int via rememberSaveable mutableIntStateOf(0), not sealed class").
 *
 * Unit-tested by AddItemModeTest (Wave 0).
 *
 * History:
 *  - Phase 11 Wave 0: introduced as 3-mode (PasteUrl default, Browse stores, Manual).
 *  - Plan 17-01: middle mode removed as part of Stores decommissioning;
 *    enum is now 2-mode (PasteUrl default, Manual).
 */
enum class AddItemMode { PasteUrl, Manual }

/** Default AddItemMode ordinal for rememberSaveable initial value. */
const val ADD_ITEM_MODE_DEFAULT_ORDINAL: Int = 0
