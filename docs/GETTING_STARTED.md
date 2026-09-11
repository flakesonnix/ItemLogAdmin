# Getting Started

> Use this template → build in 2 minutes.

## 1. Create from template

**GitHub UI:** Click **Use this template** → **Create a new repository** → Name `my-paper-plugin` → Create.

**CLI (`gh`):**

```bash
gh repo create my-paper-plugin --template flakesonnix/paper-kotlin-template --public --clone
cd my-paper-plugin
```

**Manual clone:**

```bash
git clone https://github.com/flakesonnix/paper-kotlin-template.git my-paper-plugin
cd my-paper-plugin
rm -rf .git && git init && git add . && git commit -m "init from template"
gh repo create my-paper-plugin --public --source=. --push
```

## 2. Prerequisites

- **Nix** (recommended) → provides JDK 21 + Gradle 8.14 + `jetbrains.idea` + `nixfmt`/`ktlint`
  - Install Nix: https://nixos.org/download
  - Enable flakes: `echo "experimental-features = nix-command flakes" >> ~/.config/nix/nix.conf`
- **Without Nix:** Install JDK 21 + Gradle 8+ manually.

Check:

```bash
java -version # 21
gradle --version # 8.14+ (or via nix develop)
```

## 3. First build

### With Nix

```bash
nix develop
# → Paper template — java 21... | gradle 8.14.4

gradle shadowJar
ls -lh build/libs/template-plugin-1.0.0.jar # 24M fat jar
```

### Without Nix

```bash
./gradlew shadowJar # if wrapper present, else `gradle shadowJar`
```

**Via flake:**

```bash
nix develop -c gradle shadowJar          # recommended (pure devShell)
nix build --accept-flake-config --option sandbox relaxed --impure # impure, needs network for Gradle plugins
# → result/*.jar
```

## 4. First run

**Docker DB (optional):** Only if you switch from default SQLite.

```bash
docker compose up -d # if database.type=mysql|postgresql in config.yml
```

**Paper server:**

```bash
mkdir -p run
# download Paper 1.21+ (or 1.26.2 when released) → run/paper.jar
curl -o run/paper.jar https://api.papermc.io/v2/projects/paper/versions/1.21.10/builds/.../downloads/paper-1.21.10-*.jar

mkdir -p run/plugins
cp build/libs/template-plugin-1.0.0.jar run/plugins/
cd run && java -jar paper.jar nogui
# logs: DB connected [sqlite] + DB migrate done + TemplatePlugin enabled (DB=ok)
```

**Verify:**

- In server: `ping` → `Pong! Your ping: ...ms`
- `plugins/TemplatePlugin/database.db` exists (SQLite)
- `logs/latest.log` contains `TemplatePlugin enabled`

## 5. Customize

1. **Rename package:** `com.example.template` → `me.you.plugin` (search/replace + move files + update `plugin.yml`/`paper-plugin.yml` `main`)
2. **Plugin meta:** Edit `src/main/resources/plugin.yml` + `paper-plugin.yml` (`name`, `author`, `description`, `api-version`)
3. **Version:** `build.gradle.kts` → `version = "0.1.0"`
4. **Database:** Edit `src/main/resources/config.yml` → change `database.type`
5. **Format:** `gradle spotlessApply` + `nix fmt`

## 6. Next steps

- Read [DEVELOPMENT.md](DEVELOPMENT.md) for Gradle/Nix/IDEA/formatters
- Read [DATABASE.md](DATABASE.md) for Hikari, DAOs
- Read [ARCHITECTURE.md](ARCHITECTURE.md) for project layout
- Set up CI (already in `.github/workflows/ci.yml` → push to trigger)

## Troubleshooting

- **`DataSource not initialized`** → DB connect failed, check `config.yml`
- **`No suitable driver`** → removed driver but `database.type` still points to it → add back dep
- **`Unsupported class file major version 65`** → old Shadow 8.1.1, we use manual `shadowJar` (fixed)
- **`allowUnfree` error** → flake needs `config.allowUnfree=true` (already set), run `nix build --accept-flake-config`
- **`nix build` fails plugin not found** → Gradle needs network, use `nix develop -c gradle build` (pure) or `nix build --accept-flake-config --option sandbox relaxed --impure`

Need help? Open an issue.
