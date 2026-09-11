# Configuration — ItemLogAdmin

`plugins/ItemLogAdmin/config.yml`:

```yaml
database:
  type: sqlite
  sqlite: { file: database.db } # must match ItemLog
gui:
  title: "ItemLog Admin"
  rows: 6
```

Point `database.*` to the same DB as ItemLog (same file or same mysql). No shared jar needed.
