# Database Guide

## Quick start

1. **Default (SQLite)** — zero config. DB file → `plugins/TemplatePlugin/database.db`
   ```yaml
   database:
     type: sqlite
     sqlite:
       file: database.db
   ```

2. Build → drop jar → restart. Logs: `DB connected [sqlite]` + `DB migrate done`.

3. Use in code (Kotlin):
   ```kotlin
   val db = plugin.database
   db.getConnection().use { c ->
       c.prepareStatement("SELECT 1").use { ps -> ps.executeQuery() }
   }
   ```

---

## Config options

`src/main/resources/config.yml` is auto-copied on first start via `saveDefaultConfig()`.

| key | desc | default |
|-----|------|---------|
| `database.type` | `sqlite` \| `mysql` \| `postgresql` | `sqlite` |
| `database.pool.maximum-pool-size` | Hikari max | `10` (sqlite forced `1`) |
| `database.pool.minimum-idle` | Hikari min idle | `2` |
| `database.sqlite.file` | file under `plugins/TemplatePlugin/` | `database.db` |
| `database.mysql.*` | host/port/db/user/pass/params | see config.yml |
| `database.postgresql.*` | host/port/db/user/pass/params | see config.yml |

Switch type → restart. No code change.

---

## Architecture

```
TemplatePlugin (onEnable: connect + migrate)
  └─ Database (HikariDataSource wrapper)
       ├─ handles sqlite/mysql/postgres URL + driver
       ├─ migrate() → CREATE TABLE IF NOT EXISTS
       └─ getConnection() → pooled Connection

     Repos / DAOs (example):
       ├─ PingHistoryRepository  → ping_history
       └─ PlayerDataRepository   → example_players
```

- **HikariCP** pooled. Config via `database.pool.*`.
- **SQLite** pool forced 1 (single writer) + `jdbc:sqlite:` + `org.sqlite.JDBC`.
- **Fat jar** bundles deps via manual `shadowJar` task (no relocation by default; add `org.gradle.shadow` 8.3.x + `relocate` if you need `com.zaxxer.hikari → com.example.template.libs.hikari`).

---

## Migrations

`Database.migrate()` runs on enable. Idempotent `CREATE TABLE IF NOT EXISTS`.

Add new table (Kotlin):

```kotlin
// Database.kt → migrate()
val ai = if (isSqlite()) "AUTOINCREMENT" else "AUTO_INCREMENT"
s.execute("""
  CREATE TABLE IF NOT EXISTS my_table (
    id INTEGER PRIMARY KEY $ai,
    uuid VARCHAR(36) NOT NULL,
    data TEXT
  )
""".trimIndent())
```

Production tip: extract to `db/migration/V1__init.sql` + simple runner if project grows (Flyway not bundled to keep jar small).

---

## DAO examples

### Sync (small query, already async thread)
```kotlin
pingRepo.save(player.uniqueId, player.name, player.ping)
```

### Async (never block main thread)
```kotlin
plugin.server.asyncScheduler.runNow(plugin) {
    pingRepo.save(uuid, name, ping)
    val recent = pingRepo.recent(uuid, 10)
    // back to main if need Bukkit API
    plugin.server.globalRegionScheduler.execute(plugin) {
        player.sendMessage("avg: ${pingRepo.average(uuid, 10)}")
    }
}
```

> **Rule**: DB I/O → async. Bukkit API (sendMessage, getPing) → main / region scheduler.

### Transactions
```kotlin
db.getConnection().use { c ->
    c.autoCommit = false
    try {
        c.prepareStatement("INSERT ...").use { a -> a.executeUpdate() }
        c.prepareStatement("UPDATE ...").use { b -> b.executeUpdate() }
        c.commit()
    } catch (e: SQLException) {
        c.rollback()
        throw e
    } finally {
        c.autoCommit = true
    }
}
```

### Upsert (example)
```kotlin
playerDataRepo.upsert(uuid, name)
// sqlite: ON CONFLICT(uuid) DO UPDATE
// mysql:  ON DUPLICATE KEY UPDATE
```

---

## Switching DBs

### MySQL locally (docker)
```yaml
# docker-compose.yml
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: change_me
      MYSQL_DATABASE: minecraft
    ports: ["3306:3306"]
    volumes: ["mysql_data:/var/lib/mysql"]
volumes: { mysql_data: }
```
```yaml
# config.yml
database:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    user: root
    password: change_me
```

### PostgreSQL locally
```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_PASSWORD: change_me
      POSTGRES_DB: minecraft
    ports: ["5432:5432"]
```

```yaml
database:
  type: postgresql
```

---

## Slimming jar

If you only need SQLite, remove in `build.gradle.kts`:
```kotlin
// implementation("com.mysql:mysql-connector-j:...")
// implementation("org.postgresql:postgresql:...")
```

Then `gradle shadowJar` → ~2 MB smaller.

---

## Troubleshooting

- `DataSource not initialized` → forgot `database.connect()` (already in TemplatePlugin.onEnable).
- `No suitable driver` → driver removed but config still points to that type. Add back dep or switch `database.type`.
- `database is locked` (sqlite) → concurrent writes. Already pooled size 1 helps; for high write volume switch to mysql/postgres or batch writes async.
- `Communications link failure` (mysql) → check host/port/firewall, `params` includes `allowPublicKeyRetrieval=true`.
- Slow enable → increase `connection-timeout` or check DB unreachable.

---

## Next steps

- Add Flyway/Liquibase if migrations > ~5.
- Add `docker-compose.yml` + `flake.nix` `services.mysql` for dev (ask if wanted).
- See `src/main/kotlin/com/example/template/db/` for full impl (Kotlin).
