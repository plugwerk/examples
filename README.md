# Plugwerk Examples

Example projects demonstrating plugin development with [Plugwerk](https://github.com/plugwerk/plugwerk) and the [PF4J](https://github.com/pf4j/pf4j) plugin framework.

## Examples

| Example | Description |
|---|---|
| [plugwerk-java-cli-example](plugwerk-java-cli-example/) | Standalone Java CLI application with dynamically loadable command plugins |
| [plugwerk-springboot-thymeleaf-example](plugwerk-springboot-thymeleaf-example/) | Spring Boot + Thymeleaf web application with dynamically loadable page plugins |

Each example is a self-contained Gradle project. See the `README.md` in each example directory for details.

## Prerequisites

- **Java 21** or later
- **Docker** and **Docker Compose** (optional, for running a local Plugwerk server)

## Quick Start

### 1. Start a local Plugwerk server (optional)

```bash
docker compose up -d
```

This starts the Plugwerk server (SNAPSHOT image from GHCR) and PostgreSQL. The server is available at `http://localhost:8080` with credentials `admin` / `admin`.

### 2. Build an example

```bash
cd plugwerk-java-cli-example
./gradlew build
```

## Resolving Plugwerk Dependencies

The examples resolve Plugwerk library artifacts from [GitHub Packages](https://github.com/orgs/plugwerk/packages). Authentication is required even for public packages.

### In GitHub Actions (CI)

No configuration needed — the `GITHUB_TOKEN` is automatically available and has `read:packages` scope.

### For Local Development

Create a Personal Access Token (PAT) with `read:packages` scope and add it to `~/.gradle/gradle.properties`:

```properties
gpr.user=your-github-username
gpr.key=ghp_your-personal-access-token
```

See the [Plugwerk Development Guide](https://github.com/plugwerk/plugwerk/blob/main/docs/development.md) for details.

## License

Apache-2.0 — see [LICENSE](LICENSE).
