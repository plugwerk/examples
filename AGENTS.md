# AGENTS.md

Universal AI agent instructions for the Plugwerk examples. All AI coding agents (Claude Code, GitHub Copilot, Cursor, etc.) should read this file first.

## What This Repository Is

This repository contains **example projects** demonstrating plugin development with [Plugwerk](https://github.com/plugwerk/plugwerk) and the [PF4J](https://github.com/pf4j/pf4j) plugin framework.

Two self-contained Gradle projects:

| Example | Description |
|---|---|
| `plugwerk-java-cli-example` | Standalone Java CLI application with dynamically loadable command plugins |
| `plugwerk-springboot-thymeleaf-example` | Spring Boot + Thymeleaf web application with dynamically loadable page plugins |

The main application source code lives in [plugwerk/plugwerk](https://github.com/plugwerk/plugwerk).

## Naming

Use **"Plugwerk"** (not "PlugWerk") everywhere.

## Language

All project communication is in **English**: source code, documentation, issues, PR descriptions, reviews, commit messages.

This includes **source code comments** — inline comments and Javadoc must all be written in English. German is never acceptable in source files.

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Build | Gradle 9.x (Kotlin DSL) |
| Plugin Framework | PF4J 3.x |
| CLI (cli-example) | picocli |
| Web (springboot-example) | Spring Boot 4.x + Thymeleaf |
| Formatting | Spotless + google-java-format |
| Dependency Source | GitHub Packages (SNAPSHOTs) + Maven Central (releases) |

## Build Commands

Each example is an independent Gradle build. You can either run commands from the example's directory or use the root-level composite build to drive both examples at once.

### Composite build (run from repository root)

The root `settings.gradle.kts` includes both examples as `includeBuild(...)` targets. Lifecycle tasks (`build`, `clean`, `assemble`, `check`) are aggregated so a single root-level invocation drives both examples.

```bash
./gradlew build              # Build both examples (all subprojects + tests)
./gradlew clean              # Clean both examples
./gradlew check              # Run checks in both examples
```

Per-example tasks are still reachable via their included-build paths, e.g. `./gradlew :plugwerk-springboot-thymeleaf-example:bootRun`.

### Java CLI Example (run from `plugwerk-java-cli-example/`)

```bash
./gradlew build              # Build all modules + run tests
./gradlew clean build        # Clean build
./gradlew spotlessApply      # Fix code formatting
```

### Spring Boot + Thymeleaf Example (run from `plugwerk-springboot-thymeleaf-example/`)

```bash
./gradlew build              # Build all modules + run tests
./gradlew bootRun            # Run the Spring Boot application
./gradlew clean build        # Clean build
./gradlew spotlessApply      # Fix code formatting
```

### Local Plugwerk Server (run from repo root)

```bash
docker compose up -d         # Start Plugwerk server + PostgreSQL
docker compose down          # Stop
```

Server available at `http://localhost:8080` with credentials `admin` / `admin`.

## Project Structure

```
examples/
├── CLAUDE.md
├── AGENTS.md
├── VERSION                                      # Shared version (aligned with plugwerk/plugwerk)
├── LICENSE                                      # Apache-2.0
├── license-header.txt                           # Apache-2.0 header for Spotless
├── docker-compose.yml                           # Local Plugwerk server (GHCR SNAPSHOT image)
├── settings.gradle.kts                          # Composite build: includes both examples
├── build.gradle.kts                             # Root aggregator for lifecycle tasks
├── gradlew, gradlew.bat, gradle/wrapper/        # Root Gradle wrapper for composite build
├── .github/workflows/ci.yml                     # CI: builds both examples
├── plugwerk-java-cli-example/
│   ├── build.gradle.kts                         # Root build with shared conventions
│   ├── settings.gradle.kts
│   ├── gradle/libs.versions.toml                # Version catalog
│   ├── plugwerk-java-cli-example-api/           # Extension-point interface (CliCommand)
│   ├── plugwerk-java-cli-example-app/           # Host application (picocli CLI)
│   ├── plugwerk-java-cli-example-hello-cmd-plugin/   # Example plugin: hello command
│   └── plugwerk-java-cli-example-sysinfo-cmd-plugin/ # Example plugin: sysinfo command
└── plugwerk-springboot-thymeleaf-example/
    ├── build.gradle.kts                         # Root build with Spring Boot + shared conventions
    ├── settings.gradle.kts
    ├── gradle/libs.versions.toml                # Version catalog
    ├── plugwerk-springboot-thymeleaf-example-api/    # Extension-point interface (PageContribution)
    ├── plugwerk-example-plugin-sysinfo/              # Example plugin: system info page
    └── plugwerk-example-plugin-env/                  # Example plugin: environment page
```

## Dependency Resolution

Plugwerk library artifacts are resolved from **GitHub Packages** (for SNAPSHOTs) and **Maven Central** (for releases). Both registries are configured in each example's `build.gradle.kts`.

### Plugwerk Dependencies Used

| Artifact | Purpose |
|---|---|
| `io.plugwerk:plugwerk-spi` | ExtensionPoint interfaces — required on the host classpath |
| `io.plugwerk:plugwerk-client-plugin` (classifier `pf4j`, type `zip`) | PF4J plugin ZIP — copied into `plugins/` directory at build time |

### Authentication

- **GitHub Actions (CI):** `GITHUB_TOKEN` is automatically available with `read:packages` scope
- **Local development:** Requires a PAT with `read:packages` scope in `~/.gradle/gradle.properties`:

```properties
gpr.user=your-github-username
gpr.key=ghp_your-personal-access-token
```

See the [Plugwerk Development Guide](https://github.com/plugwerk/plugwerk/blob/main/docs/development.md) for details.

## Git Workflow

### Mandatory pre-commit checks

Before **every commit**, run the formatter for the example(s) you changed:

```bash
cd plugwerk-java-cli-example && ./gradlew spotlessApply
cd plugwerk-springboot-thymeleaf-example && ./gradlew spotlessApply
```

CI enforces formatting and will fail if violations are committed.

- **Never commit directly to `main`** — always use a feature branch
- Branch naming: `feature/<issue-id>_<short-description>` (e.g. `feature/42_add-update-example`)
  - Issues are tracked in the main repository: [plugwerk/plugwerk](https://github.com/plugwerk/plugwerk/issues)
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `chore:`, etc.)
- AI-generated commits include a `Co-Authored-By` trailer
- One logical change per PR; reference related issues with `Relates to plugwerk/plugwerk#N` in the PR body

## Licensing

This repository is licensed under **Apache-2.0**.

Every Java source file **must** begin with the Apache-2.0 license header. Spotless is configured per-example to enforce this automatically using `license-header.txt` in the repository root.

```
/*
 * Copyright (c) 2025-present devtank42 GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

Run `./gradlew spotlessApply` to auto-fix missing or incorrect headers.

## Issue Management

Issues for these examples are tracked in the main repository: [plugwerk/plugwerk](https://github.com/plugwerk/plugwerk/issues).

## Pull Request Requirements

Every pull request **must** be created with:

- **Labels** — at minimum one label matching the change type (e.g. `enhancement`, `chore`, `documentation`)
- **Milestone** — the current milestone

### Adding commits to an existing PR

Before pushing follow-up commits to a feature branch, **always verify that the existing PR is still open**:

```bash
gh pr view <number> --json state,mergedAt
```

- If `state` is `OPEN` → push the new commits to the existing branch as usual.
- If `state` is `MERGED` or `CLOSED` → **do not** push to the merged branch. Instead:
  1. Switch to `main` and pull the latest state.
  2. Create a new feature branch (`feature/<issue-id>_<short-description>` or `fix/<short-description>`).
  3. Cherry-pick or re-apply the commits on the new branch.
  4. Open a new PR that references the original via `Follow-up to #<original-pr>`.

Rationale: pushing to a merged PR's branch does not reopen the PR; the commits end up orphaned on a stale branch and are never reviewed or merged.

## Key Design Constraints

- **Each example is self-contained** — it builds independently with its own `settings.gradle.kts` and Gradle wrapper. Do not merge them into a single Gradle project. The root-level composite build (`settings.gradle.kts` + `includeBuild(...)`) aggregates lifecycle tasks across both examples without collapsing them into one multi-module build.
- **PF4J plugin packaging** — each example plugin builds a PF4J-compatible ZIP with `MANIFEST.MF` at the root and JARs in `lib/`. Host-provided dependencies (plugwerk-spi, pf4j, slf4j) are excluded from the ZIP.
- **plugwerk-client-plugin is a runtime dependency** — it is loaded as a PF4J plugin from the `plugins/` directory, not as a compile dependency. The `copyClientPlugin` task downloads the PF4J ZIP artifact from GitHub Packages.
- **Extension-point API pattern** — each example defines its own API module (e.g. `plugwerk-java-cli-example-api`) that both the host app and plugins depend on. This is the standard PF4J pattern for type-safe extension points.

## References

- [plugwerk/plugwerk](https://github.com/plugwerk/plugwerk) — Main Plugwerk repository
- [ADR-0017](https://github.com/plugwerk/plugwerk/blob/main/docs/adrs/0017-dual-registry-publishing-strategy.md) — Dual-registry publishing strategy
- [Development Guide](https://github.com/plugwerk/plugwerk/blob/main/docs/development.md) — SNAPSHOT resolution setup
- [PF4J Documentation](https://pf4j.org/) — Plugin framework documentation
