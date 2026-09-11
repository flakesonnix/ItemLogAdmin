# Database — ItemLogAdmin

Reads `item_events` (created by ItemLog) and writes `restorations`.

Repos: `ItemLogQueryRepository` (select with filters), `PlayerRepository` (distinct players), `RestorationRepository` via `RestoreService`.

Keep ItemLog and ItemLogAdmin on same DB — that's the integration.
