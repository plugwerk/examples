# Plugwerk Examples

Example projects demonstrating plugin development with [Plugwerk](https://github.com/plugwerk/plugwerk) and the [PF4J](https://github.com/pf4j/pf4j) plugin framework.

## Examples

| Example | Description |
|---|---|
| [plugwerk-java-cli-example](plugwerk-java-cli-example/) | Standalone Java CLI application with dynamically loadable command plugins |
| [plugwerk-springboot-thymeleaf-example](plugwerk-springboot-thymeleaf-example/) | Spring Boot + Thymeleaf web application with dynamically loadable page plugins |

Each example is a self-contained Gradle build. The repository root also provides a Gradle **composite build** that drives both examples at once. See each example's own `README.md` for in-depth details.

## Prerequisites

- **Java 21** or later
- **Docker** and **Docker Compose** (optional, for running a local Plugwerk server)
- **GitHub Personal Access Token** with `read:packages` scope — only required to resolve Plugwerk SNAPSHOT artifacts from GitHub Packages for Maven (see [Resolving Plugwerk dependencies](#resolving-plugwerk-dependencies))

## Quick start

### 1. Start a local Plugwerk server (optional)

```bash
docker compose up -d
```

Starts the Plugwerk server (SNAPSHOT image from GHCR) and PostgreSQL. Available at `http://localhost:8080` with credentials `admin` / `admin`.

The container image `ghcr.io/plugwerk/plugwerk-server:snapshot` is **publicly pullable** — no `docker login` required.

> Health check: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.
> The server has no web frontend under `/`; use the REST API under `/api/v1/...` or OpenAPI under `/v3/api-docs` (auth required).

### 2. Build both examples (composite build)

From the repository root:

```bash
./gradlew build              # Build both examples with tests
./gradlew clean              # Clean both examples
./gradlew check              # Run checks in both examples
```

Per-example tasks remain reachable via path, e.g.:

```bash
./gradlew :plugwerk-springboot-thymeleaf-example:bootRun
```

### 3. Build a single example

Each example is also a fully standalone Gradle build:

```bash
cd plugwerk-java-cli-example
./gradlew build
```

## Resolving Plugwerk dependencies

Plugwerk ships artifacts from two registries:

| Artifact type | Registry | Auth required |
|---|---|---|
| Releases (e.g. `1.0.0`) | Maven Central | No |
| SNAPSHOTs (e.g. `1.0.0-SNAPSHOT`) | GitHub Packages for Maven | **Yes — `read:packages` token** |

The examples currently consume **SNAPSHOT** artifacts during active development, so a token is needed for `./gradlew build` in the current setup.

> **Why is a token needed even though the source repo is public?**
> GitHub Packages for Maven requires authentication for downloads regardless of package visibility — a long-standing limitation that only GHCR (container registry) has fixed. Once Plugwerk switches to release coordinates from Maven Central, no token will be required.

### In GitHub Actions (CI)

No configuration needed — the built-in `GITHUB_TOKEN` already has `read:packages` scope.

### For local development

1. Create a **Classic Personal Access Token** at https://github.com/settings/tokens with the `read:packages` scope.
2. Add it to `~/.gradle/gradle.properties`:

   ```properties
   gpr.user=your-github-username
   gpr.key=ghp_your-personal-access-token
   ```

Alternatively, set `GITHUB_ACTOR` and `GITHUB_TOKEN` environment variables — the build scripts fall back to those.

See the [Plugwerk Development Guide](https://github.com/plugwerk/plugwerk/blob/main/docs/development.md) for details.

## License

Apache-2.0 — see [LICENSE](LICENSE).
