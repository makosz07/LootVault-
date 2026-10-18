# LootVault redesign: architecture and acceptance criteria

The existing 0.3.x application is a prototype. Work on the `redesign/material3` branch until a replacement passes testing. Do not overwrite users' data or publish the branch as a production release.

## User experience
- Material 3 UI, accessible contrast, responsive typography and large touch targets.
- Bottom navigation entirely above Android gesture/navigation bar using `WindowInsets`; status-bar-safe header. Test Android 13, 14, 15 and 16, gesture and three-button navigation.
- Home: total asset value, purchase cost, unrealized gain/loss, asset counts, recent items, time-series chart.
- Resources: prominent All Assets card, configurable image-cover cards for resource types, custom categories, search/filter/sort.
- Item details: photo, quantity, purchase cost, current valuation, notes, storage location, dates, editable custom fields, valuation timeline.
- Settings: PL/EN/ES, JSON import/export, About (app name/version).

## Financial correctness
- Decimal arithmetic, not float/double, for currency. Unit price x quantity.
- Gain/loss = current market value - acquisition cost; percentage uses acquisition cost as denominator. Undefined percentage when acquisition cost is zero.
- Record timestamped valuations per item. Do not overwrite prior values.
- Record daily portfolio snapshots. Distinguish changes from purchases/disposals from revaluation performance.
- Demo entries must be clearly labeled and excluded from real portfolio statistics unless user opts in.
- Display missing valuations as unknown, not automatically as 0 zł.
- Provide manual valuation first; external market prices can be optional later.

## Persistence and migration
- Room/SQLite with entities for asset types, categories, assets, valuations, portfolio snapshots and photo references.
- Migration from legacy SharedPreferences JSON before removing old storage. Preserve UUIDs and history.
- Backup: versioned JSON schema with import preview and validation. Photos require a separate bundled archive or embedded content; JSON-only export must explicitly disclose that photos are excluded.
- Never silently overwrite data or delete a collection. Make deletion confirmable.

## Release checks
- Unit tests for money, percentages, quantities, aggregate values and historical series.
- UI tests for insets, keyboard resizing, scroll-to-save and bottom navigation.
- APK built by GitHub Actions; verify successful build and install before sharing.
- Real APK size is not a quality metric; do not pad files to reach 1 MB.
