# ItemLogAdmin

Paper 1.26.2 — Kotlin — admin GUI for ItemLog.

`/itemlog` → player list → events → details → restore. Search by name/UUID, filter by type/time, paginate, preview item, confirm, audit via `restorations`.

```bash
gradle shadowJar
# → build/libs/itemlogadmin-1.0.0-SNAPSHOT.jar
```

Uses the same DB as ItemLog — no shared code, DB is the contract. Point both plugins at the same `database.db` (or same MySQL).

## Commands

- `/itemlog [player]` — open panel (`itemlog.admin`, default op)
- aliases: `/il`, `/itemlogadmin`

## Config

`plugins/ItemLogAdmin/config.yml`:

```yaml
database:
  type: sqlite
  sqlite: { file: database.db } # same file as ItemLog
gui:
  title: "ItemLog Admin"
  rows: 6
```

Set `database.*` to match ItemLog's DB.

## Dev

```bash
nix develop
gradle shadowJar
nix fmt
```

Pair with `ItemLog`. See `docs/` for query and restore flow.
