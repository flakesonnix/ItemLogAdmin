# Architecture

## Overview

```
Paper 1.26.2 (JVM)
       │
       ▼
TemplatePlugin.kt (JavaPlugin)
       │
       ├─ Database.kt (HikariDataSource) ──► HikariCP pool
       │      ├── sqlite (default, file, pool 1)
       │      ├── mysql (Hikari, pool 10)
       │      └── postgresql (Hikari)
       │      └── migrate() → CREATE TABLE IF NOT EXISTS
       │
       ├─ PingHistoryRepository.kt ──► ping_history (uuid, name, ping, at)
       ├─ PlayerDataRepository.kt  ──► example_players (uuid, name, last_seen)
       │
       ├── PlayerJoinListener.kt (async upsert on join)
       └── PingCommand.kt (CommandExecutor+TabCompleter, async save)
```

## Modules

| Module | File | Role |
|--------|------|------|
| **Plugin** | `TemplatePlugin.kt` | `onEnable`: `saveDefaultConfig`, `Database.connect`+`migrate`, init repos, register `PlayerJoinListener` + `PingCommand`; `onDisable`: `close` pool |
| **DB** | `db/Database.kt` | Config-driven `HikariConfig` (type `sqlite`/`mysql`/`postgresql`), `getConnection()`, `isConnected()`, `isSqlite()`, `migrate()`, `close()` |
| **DAO 1** | `db/PingHistoryRepository.kt` | `save(uuid,name,ping)`, `recent(uuid,limit):List<Entry>`, `average(uuid,n):Double` — `data class Entry` |
| **DAO 2** | `db/PlayerDataRepository.kt` | `upsert(uuid,name)` — `ON CONFLICT` (sqlite) / `ON DUPLICATE KEY` (mysql) |
| **Command** | `PingCommand.kt` | `/ping [player]` — perm checks, `asyncScheduler.runNow { repo.save+upsert }` |
| **Listener** | `PlayerJoinListener.kt` | `onJoin` → async `upsert` |
| **Config** | `config.yml` | `database.*`, `ping.save-history`, pool tuning |
| **Flake** | `flake.nix` | `devShell` (jdk21+gradle+idea+nixfmt/ktlint), `formatter` (nixfmt), `packages.idea` (jetbrains.idea), `packages.default` (gradle build, impure) |
| **Build** | `build.gradle.kts` | kotlin-jvm 2.0.21 + spotless(ktlint) + idea + paper-api + Hikari/deps + manual `shadowJar` |

## Data Flow

1. **Enable:** `saveDefaultConfig` → `Database(type)` reads `config.yml` → `HikariConfig` → `HikariDataSource` → `migrate()` (CREATE TABLE)
2. **Join:** `PlayerJoinEvent` (main) → `asyncScheduler.runNow` → `PlayerDataRepository.upsert` (upsert player)
3. **Command:** `/ping` (main → `getPing()` ) → `sendMessage` (main) → `asyncScheduler.runNow` → `PingHistoryRepository.save` + `PlayerDataRepository.upsert` (async, pooled)
4. **Disable:** `Database.close()` → pool closed

## Threading

- **Main/Region** → Bukkit API (`getPing()`, `sendMessage`, `getPlayer()`, events, commands)
- **Async** → DB I/O (`getConnection()`, `prepareStatement`, `execute`) via `server.asyncScheduler.runNow(plugin) { ... }` — never block main

```kotlin
// correct
server.asyncScheduler.runNow(plugin) {
    pingHistory.save(uuid, name, ping)
    server.globalRegionScheduler.execute(plugin) {
        player.sendMessage("avg ${pingHistory.average(uuid, 10)}")
    }
}
```

## Build

- **Kotlin-JVM** — Paper API is Java, so Kotlin is JVM (`jvmTarget 21`), not Kotlin/Native (Native cannot be loaded as Paper plugin). Tooling prefers native where possible (`nixfmt`, `ktlint` CLI), but plugin stays JVM.
- **Fat jar** — Manual `shadowJar` (`Jar` + `zipTree(runtimeClasspath)`). No relocation by default; add Shadow 8.3.x + `relocate` if you need isolation (`com.zaxxer.hikari` → `libs`).
- **Paper version** — `paperVersion` prop defaults `1.21.10` (1.26.2 not yet on repo). Bump via `-PpaperVersion=1.26.2-R0.1-SNAPSHOT`.
- **API version** — `plugin.yml`/`paper-plugin.yml` `api-version: '1.21'` → bump when Paper 1.26 requires.

## Nix

- `devShell` is source of truth for toolchain (reproducible JDK 21 + Gradle 8)
- `formatter = pkgs.nixfmt` → `nix fmt` (native)
- `packages.idea = ideaPkg` → `nix run .#idea` (jetbrains.idea, unfree)
- `packages.default` → `gradle build` (needs network + `sandbox=relaxed` + `--accept-flake-config --impure`) — prefer `nix develop -c gradle build` for pure local

## Decisions

- **HikariCP** — battle-tested pool, small, Paper-compatible
- **SQLite default** — zero-config, single file, `pool 1` (single-writer)
- **Manual shadowJar** — avoids Shadow 8.1.1 ASM 65 bug (Java 21 class), native where possible
- **Spotless + ktlint** — official Kotlin style, `.editorconfig` + IDEA sync, CI enforced
- **IDEA committed** — `gradle.xml`, `misc.xml`, `codeStyles`, `runConfigurations` kept, `workspace.xml` ignored
