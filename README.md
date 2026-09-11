# Paper Kotlin Template — 1.26.2

[![CI](https://github.com/flakesonnix/paper-kotlin-template/actions/workflows/ci.yml/badge.svg)](https://github.com/flakesonnix/paper-kotlin-template/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Paper](https://img.shields.io/badge/Paper-1.26.2-0288D1?logo=minecraft)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://openjdk.org)
[![Nix](https://img.shields.io/badge/Nix-flake-5277C3?logo=nixos)](https://nixos.org)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

Minimal, production-ready **Paper** plugin template in **Kotlin** — `/ping` command, **HikariCP** database layer (SQLite/MySQL/PostgreSQL), **Gradle**, **Nix flake** (`nix build` / `nix develop`), **Spotless** (`ktlint`) + **nixfmt**, **JetBrains IDEA** ready, **CI**.

> **Template usage:** Click **Use this template** → Create repo → `git clone` → `nix develop` → `gradle shadowJar`. See [Getting Started](docs/GETTING_STARTED.md).

---

## Features

- **Paper 1.26.2** (Kotlin-JVM, `apiVersion 1.21`, param `paperVersion` for future bump) — `Paperclip` ready
- **Kotlin 2.0.21**, `jvmTarget 21`, toolchain 21 — idiomatic, null-safe, no Java fallback
- **`/ping`** — self/others, perms `template.ping`/`template.ping.others`, async DB save, tab-complete
- **Database** — HikariCP pool, `sqlite` (default file), `mysql`, `postgresql`, auto `migrate()`, DAOs `PingHistoryRepository` / `PlayerDataRepository`, async via `asyncScheduler`
- **Nix flake** — `jdk21` + `gradle_8` + `jetbrains.idea` (unfree) + `nixfmt`/`ktlint` native; `nix develop`, `nix build` (impure, `sandbox=relaxed` for Gradle network), `nix fmt`, `nix run .#idea`
- **Formatter** — Kotlin `Spotless 7.0.2 + ktlint 1.5.0` (`gradle spotlessApply/Check`), Nix `nixfmt` (`nix fmt`), `.editorconfig`, IDEA `KOTLIN_OFFICIAL` code style
- **IDEA** — `gradle idea` + committed `.idea/` (`gradle.xml`, `misc.xml` JDK21, `modules.xml`, `vcs.xml`, `codeStyles/`, `runConfigurations/` for `shadowJar` & `Paper Run`)
- **CI** — GitHub Actions: `gradle build`, `spotlessCheck`, `nix fmt --check`, `nix build` (see [.github/workflows/ci.yml](.github/workflows/ci.yml))
- **Git only** — no external generators, clean history

---

## Requirements

- **JDK 21** (Paper 1.26+ requires 21)
- **Gradle 8.14+** *or* **Nix** (`nix develop` provides JDK + Gradle + IDEA + formatters)
- **Kotlin 2.0.21** (via Gradle)
- DB: *none* for SQLite (default), Docker for MySQL/PostgreSQL (`docker compose up -d`)

---

## Quick Start

### 1. Use template

```bash
gh repo create my-plugin --template flakesonnix/paper-kotlin-template --public --clone
# or: Use this template button on GitHub
cd my-plugin
```

### 2. Develop (Nix)

```bash
nix develop
# → Paper template — java 21.0.12 | gradle 8.14.4
# → gradle shadowJar → build/libs/template-plugin-1.0.0.jar

gradle shadowJar
# jar → build/libs/template-plugin-1.0.0.jar
```

### 2b. Without Nix

```bash
./gradlew shadowJar   # or gradle shadowJar (JDK 21 required)
```

### 3. Run

```bash
cp build/libs/template-plugin-1.0.0.jar /path/to/paper/plugins/
# or local run dir:
mkdir -p run/plugins && cp build/libs/*.jar run/plugins/ && java -jar paper.jar nogui
```

Logs: `DB connected [sqlite]` + `DB migrate done` + `TemplatePlugin enabled (DB=ok)`.

### 4. Commands

- `/ping` — your latency (saves to DB if `ping.save-history: true`)
- `/ping <player>` — other's latency (`template.ping.others`, default op)
- Perm `template.ping` default `true`

---

## Configuration

`src/main/resources/config.yml` copied via `saveDefaultConfig()`:

```yaml
database:
  type: sqlite # sqlite | mysql | postgresql
  sqlite: { file: database.db } # → plugins/TemplatePlugin/database.db
  pool: { maximum-pool-size: 10, minimum-idle: 2, ... }

ping:
  save-history: true
```

Switch DB → change `database.type` → restart. No code change.

Details → [Database Guide](docs/DATABASE.md) + [Configuration](docs/CONFIGURATION.md) + [`docker-compose.yml`](docker-compose.yml).

---

## Project Structure

```
template-plugin/
├── flake.nix               # jdk21, gradle, jetbrains.idea (unfree), nixfmt/ktlint, devShell + packages.idea + formatter + packages.default (gradle build)
├── build.gradle.kts        # kotlin-jvm 2.0.21 + spotless(ktlint) + paper-api:$paperVersion + HikariCP/sqlite/mysql/pg + manual shadowJar
├── settings.gradle.kts
├── gradle.properties
├── src/main/kotlin/        # Kotlin-JVM (Paper is JVM-only)
│   └── com/example/template/
│       ├── TemplatePlugin.kt
│       ├── PingCommand.kt
│       ├── PlayerJoinListener.kt
│       └── db/Database.kt, PingHistoryRepository.kt, PlayerDataRepository.kt
├── src/main/resources/
│   ├── plugin.yml / paper-plugin.yml (Paper prefers paper-plugin.yml)
│   └── config.yml
├── .idea/                  # committed: gradle.xml, misc.xml (JDK21), modules.xml, vcs.xml, codeStyles/, runConfigurations/
├── docs/                   # English docs
└── .github/workflows/ci.yml
```

---

## Development

```bash
nix develop
gradle spotlessApply   # Kotlin fmt (ktlint 1.5.0, native)
gradle spotlessCheck   # check
nix fmt                # Nix fmt (nixfmt native)
gradle shadowJar       # fat jar (manual, no shadow ASM issue)
gradle build           # also shadowJar
gradle idea            # generate .idea/.iml
nix run .#idea         # IDEA via nix (jetbrains.idea)
nix build --accept-flake-config --option sandbox relaxed --impure # impure gradle fetch (or use nix develop)
```

- **Kotlin native vs Java:** Plugin is **Kotlin-JVM** (Paper API is JVM-only, Kotlin/Native cannot be loaded). Tooling prefers native where possible: `nixfmt` (Rust native), `ktlint` native CLI (`pkgs.ktlint`), but Gradle Spotless wrapper is JVM-required for Paper.
- **Formatter:** `.editorconfig` (Kotlin 4, Nix 2, 120 cols off for SQL) + `.idea/codeStyles/Project.xml` (`KOTLIN_OFFICIAL`). CI enforces both.
- **IDEA:** `nix develop -c idea . &` or `nix run .#idea`. Run configs: *Build shadowJar* (Gradle), *Paper Run (1.26.2)* (requires `run/paper.jar`).
- **DB:** See [DATABASE.md](docs/DATABASE.md) for Hikari, migrations, DAOs, transactions, docker.

---

## Building via Flake

- **Dev shell (recommended):** `nix develop -c gradle shadowJar` — pure, network allowed, no sandbox issues.
- **Nix build (impure):** `nix build --accept-flake-config --option sandbox relaxed --impure` → `result/*.jar` (needs network for Gradle plugins, `nixConfig.sandbox=relaxed`, `allowUnfree=true` for IDEA). Pure `nix build` without flags fails due to sandbox + unfree + Gradle Central fetch — use dev shell for CI/local.
- **CI** uses `nix develop --command gradle build` (see `ci.yml`).

---

## CI

GitHub Actions — `.github/workflows/ci.yml`:

- Ubuntu latest, JDK 21, Gradle, Nix (`cachix/install-nix-action`)
- Steps: `gradle spotlessCheck`, `nix fmt --check`, `nix develop --command gradle shadowJar`, `nix build` (relaxed, impure)

Badge above links to runs.

---

## Docs

- [Getting Started](docs/GETTING_STARTED.md) — template → first build → first run
- [Development](docs/DEVELOPMENT.md) — Nix, Gradle tasks, IDEA, formatters
- [Database](docs/DATABASE.md) — Hikari, sqlite/mysql/pg, DAOs, migrations
- [Architecture](docs/ARCHITECTURE.md) — modules, flow
- [Contributing](docs/CONTRIBUTING.md) — PR, commit style

All docs in **English**.

---

## Template

This repo is a **GitHub template** (`gh repo edit --template`). Create from it via `Use this template` or `gh repo create --template flakesonnix/paper-kotlin-template`.

---

## License

[MIT](LICENSE) — do what you want, keep header.

---

*Built for Paper 1.26.2 — Kotlin 2.0.21 — Flake. PRs welcome.*
