# Configuration

`src/main/resources/config.yml` is copied to `plugins/TemplatePlugin/config.yml` on first run via `saveDefaultConfig()`. Edit there, then `/reload` or restart.

## Defaults

```yaml
database:
  type: sqlite          # sqlite | mysql | postgresql
  pool:
    maximum-pool-size: 10   # sqlite forced 1
    minimum-idle: 2
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
  sqlite:
    file: database.db   # → plugins/TemplatePlugin/database.db
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    user: root
    password: "change_me"
    params: "useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4&serverTimezone=UTC"
  postgresql:
    host: localhost
    port: 5432
    database: minecraft
    user: postgres
    password: "change_me"
    params: "sslmode=disable"

ping:
  save-history: true
  max-history-per-player: 1000  # 0 = keep all (not yet enforced, for future pruning)
```

## Database Type

- **sqlite** (default) — no setup, file-based, single-writer (`pool 1`), good for single server.
- **mysql** — for Bungee/Velocity/network, high write volume.
- **postgresql** — alternative to mysql, `ON CONFLICT` upsert.

Change `database.type` → restart. No code change. See [DATABASE.md](DATABASE.md) for docker.

## Pool Tuning

HikariCP `database.pool.*` — see [HikariCP docs](https://github.com/brettwooldridge/HikariCP).

- `maximum-pool-size` — 10 default, `1` for sqlite
- `minimum-idle` — keep at least N idle
- `connection-timeout` — wait for connection
- `idle-timeout`/`max-lifetime` — connection lifecycle

## Ping

- `ping.save-history` — if `false`, `/ping` still shows latency but not saved to DB
- Future: `max-history-per-player` pruning (not yet implemented — add scheduled task to `DELETE ... WHERE`)

## Plugin Meta

`src/main/resources/plugin.yml` + `paper-plugin.yml` (Paper prefers `paper-plugin.yml`):

```yaml
name: TemplatePlugin
version: '1.0.0' # → ${project.version} via processResources if you add filtering
main: com.example.template.TemplatePlugin
api-version: '1.21' # bump for Paper 1.26 if required
```

Change `name`/`main`/`author` when forking.

## Environment

- `config.yml` supports env substitution via `System.getenv` if you add logic (not yet) — or use `password: ${DB_PASSWORD}`
- For Docker, set `database.mysql.password` to same as `docker-compose.yml` `MYSQL_ROOT_PASSWORD`
