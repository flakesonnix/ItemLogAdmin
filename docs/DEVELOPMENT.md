# Development Guide

## Toolchain

- **JDK 21** — Paper 1.26.2 requires 21 (toolchain 21, `jvmTarget 21`)
- **Kotlin 2.0.21** — JVM (Paper is JVM-only; Kotlin/Native cannot be loaded as Paper plugin)
- **Gradle 8.14.4** — Kotlin DSL, `shadowJar` manual fatJar
- **Nix flake** — `devShell` provides `jdk21` + `gradle_8` + `jetbrains.idea` + `nixfmt`/`ktlint` native

## Nix

```bash
nix develop
# provides: java, gradle, idea, nixfmt, ktlint

nix develop -c gradle shadowJar
nix develop -c gradle spotlessCheck
nix fmt                # nixfmt native via flake `formatter`
nix run .#idea         # jetbrains.idea (unfree, allowUnfree=true)
nix build --accept-flake-config --option sandbox relaxed --impure # impure gradle fetch
nix flake check --all-systems
```

`flake.nix` highlights:

- `ideaPkg = pkgs.jetbrains.idea or pkgs.jetbrains.idea-community` (unfree, `nixConfig.allowUnfree=true`)
- `nixFmt = pkgs.nixfmt` (native, not Java)
- `formatter = nixFmt` → `nix fmt`
- `nixConfig.sandbox = "relaxed"` + `__noChroot` for `nix build` network (otherwise use `nix develop`)

## Gradle Tasks

```bash
gradle shadowJar       # 24M fat jar → build/libs/template-plugin-1.0.0.jar (manual, no ASM issue)
gradle build           # also shadowJar
gradle spotlessApply   # Kotlin fmt (ktlint 1.5.0, KOTLIN_OFFICIAL, .editorconfig)
gradle spotlessCheck   # check
gradle idea            # generate .idea/.iml
gradle clean
./gradlew -PpaperVersion=1.26.2-R0.1-SNAPSHOT shadowJar # bump Paper when released
```

`build.gradle.kts` notes:

- `plugins { kotlin("jvm") 2.0.21, idea, com.diffplug.spotless 7.0.2 }`
- `paperVersion` param defaults `1.21.10-R0.1-SNAPSHOT` (1.26.2 not yet on repo.papermc.io) → pass `-PpaperVersion=...` once published
- `implementation` includes `kotlin-stdlib`/`reflect`, `HikariCP`, `sqlite-jdbc`, `mysql-connector-j`, `postgresql`, `slf4j-api`
- `java { toolchain 21 }`, `kotlin { jvmToolchain 21 }`, `jvmTarget 21`
- Manual `shadowJar` (`Jar` + `zipTree(runtimeClasspath)` + `DuplicatesStrategy.EXCLUDE`) — no relocation; add `org.gradle.shadow` 8.3.x if you need `relocate`

## Formatter

- **Kotlin** → Spotless + ktlint 1.5.0 (`KOTLIN_OFFICIAL`) — config in `build.gradle.kts` (`editorConfigOverride` 4 spaces, `max-line-length off`, `no-wildcard-imports disabled`) + `.editorconfig` (Kotlin 4, Nix 2, etc) + `.idea/codeStyles/Project.xml` (KOTLIN_OFFICIAL)
  - Check: `gradle spotlessCheck`
  - Fix: `gradle spotlessApply`
  - Native CLI fallback: `ktlint` (pkgs.ktlint) in devShell, but prefer Gradle task
- **Nix** → `nixfmt` native (Rust) via `flake.nix` `formatter`
  - Check: `nix fmt -- --check` or `nixfmt --check flake.nix`
  - Fix: `nix fmt`
- **Why native?** Nix fmt = native (`nixfmt`), Kotlin fmt = `ktlint` native binary where possible; plugin itself must be **Kotlin-JVM** (Paper is JVM-only, Kotlin/Native incompatible).

`.editorconfig` is synced with both formatters and IDEA.

## IDEA (JetBrains)

- `gradle idea` generates `.iml`; committed `.idea/` includes:
  - `gradle.xml` (GRADLE, JDK 21), `misc.xml` (JDK 21), `modules.xml` (`template-plugin.iml`), `vcs.xml` (Git)
  - `codeStyles/Project.xml` (`KOTLIN_OFFICIAL`), `codeStyleConfig.xml` (per-project)
  - `runConfigurations/Build__shadowJar.xml` (Gradle `shadowJar`), `Paper_Run.xml` (Application `Paperclip`, `run/`), `Gradle__idea.xml`
- Open: `nix develop -c idea . &` (or `nix run .#idea`, `idea-community` fallback)
- `.gitignore` keeps `runConfigurations/` + `codeStyles/` + `gradle.xml`/`modules.xml`/`misc.xml`/`vcs.xml`, ignores `workspace.xml`/`tasks.xml`/`shelf/`
- JDK 21 auto-detected; if IntelliJ shows wrong JDK → `File → Project Structure → SDK 21`

## Database

See [DATABASE.md](DATABASE.md) — HikariCP, `config.yml`, DAOs, migrations, docker.

## Testing

- No tests yet → `gradle test` is `NO-SOURCE` (add `src/test/kotlin/` as needed)
- Add `testImplementation("io.mockk:mockk")` / `org.junit.jupiter:junit-jupiter` for unit tests
- For Paper integration tests, consider `MockBukkit` (but prefer unit + manual server run)

## Git

- `main` is default, `git only` (no extra generators)
- Commit style: Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`), bisectable, focused
- Formatter enforced in CI → run `gradle spotlessApply && nix fmt` before push
