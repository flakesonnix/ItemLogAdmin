# ItemLogAdmin

Paper 1.26.2 — Kotlin — GUI for ItemLog.

`/itemlog` → player list → events → details → restore.

Features: search (name/UUID), filter (type/time), pagination, item preview, confirm, audit (`restorations`).

```bash
gradle shadowJar
# → build/libs/itemlogadmin-1.0.0-SNAPSHOT.jar
```

Uses same DB as ItemLog. No shared code — DB is contract.
