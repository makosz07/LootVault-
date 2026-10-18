# LootVault

Android personal asset catalogue by Macosh Apps.

## What it does
- Catalogues physical and digital assets in your own asset types, each with a custom cover.
- Tracks quantity, purchase cost, current value per unit, location, notes and photos.
- Keeps a timestamped valuation history per asset and a daily portfolio snapshot.
- Shows gain and loss in currency and in percent of acquisition cost (undefined when cost is zero).
- Languages: Polski, English, Español. Backup as JSON with a preview before restore.

## Data safety
- All money is handled as `BigDecimal`, never as binary floating point.
- Saves are atomic. A vault that cannot be read is moved aside, not overwritten.
- Sample entries are marked as demo and excluded from all totals; they can be removed in Settings.

## Build
GitHub Actions > Build Android APK > Run workflow, or push to `main` / `redesign/material3`.
The workflow runs unit tests (`testDebugUnitTest`) and then builds the debug APK, which is
available as a workflow artifact.
