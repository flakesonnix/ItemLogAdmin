# Contributing

## Workflow

1. Fork → `git clone` → `nix develop` (or JDK 21 + Gradle)
2. Branch: `feat/my-feature` or `fix/bug`
3. Code in Kotlin (`src/main/kotlin/`) — keep null-safe, async DB
4. Format: `gradle spotlessApply && nix fmt` (CI checks both)
5. Build: `gradle shadowJar` (or `nix develop -c gradle shadowJar`)
6. Test: local Paper `run/` → `java -jar paper.jar nogui` → verify `/ping`
7. Commit: Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`), focused, bisectable
8. Push → PR → CI must pass

## Code Style

- **Kotlin** — `KOTLIN_OFFICIAL` via `.idea/codeStyles/Project.xml` + `.editorconfig` (4 spaces, no wildcard imports, `max-line-length off` for SQL, `continuation_indent 4`)
  - Enforced: `gradle spotlessCheck` (ktlint 1.5.0)
  - Fix: `gradle spotlessApply`
- **Nix** — `nixfmt` via `flake.nix` `formatter`
  - Check: `nix fmt -- --check` (or `nixfmt --check flake.nix`)
  - Fix: `nix fmt`
- **IDEA** — JDK 21, `gradle.xml`/`misc.xml` already committed

## Commit

- Single clean commit per PR if possible, but keep history logical
- Message: `feat: add /ping history limit` (≤50 chars subject, body explains why)
- `git add` only intended files (no `build/`/`result`)

## CI

`.github/workflows/ci.yml` runs on push/PR to `main`:

- `actions/checkout`, `cachix/install-nix-action`, `actions/setup-java` (21)
- `nix develop --command gradle spotlessCheck`
- `nix fmt -- --check` (or `nixfmt --check`)
- `nix develop --command gradle shadowJar` (and `nix build` relaxed impure)

Badge in README must stay green.

## Adding Dependencies

- Edit `build.gradle.kts` → `implementation("group:artifact:version")`
- If you need relocation → add `org.gradle.shadow` 8.3.x + `relocate` block (currently manual `shadowJar` has no relocate)
- If driver only for one DB → keep but document slim: remove unused `mysql-connector-j`/`postgresql` to save ~2M

## Database Migrations

- Add `CREATE TABLE` in `Database.kt` `migrate()` → `CREATE TABLE IF NOT EXISTS`
- Use helper: `val ai = if (isSqlite()) "AUTOINCREMENT" else "AUTO_INCREMENT"`
- For >5 migrations → add Flyway/Liquibase (not bundled to keep jar small)

## Paper Version

- Default `paperVersion = 1.21.10` (1.26.2 not yet on repo). Bump in `build.gradle.kts` or via `-PpaperVersion=...`
- Also bump `api-version` in `plugin.yml`/`paper-plugin.yml` when Paper requires
