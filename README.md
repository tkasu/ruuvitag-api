# Ruuvitag API

WIP Program to persist and get measurements from Ruuvi tags.

## Prerequisites

- **Java 25 LTS** (Eclipse Temurin) - Install via sdkman: `sdk install java 25.0.0-tem`
- **SBT 1.11.7** (specified in project/build.properties)
- **Scala 3.7.3** (managed by SBT)

## Development

This project includes a Makefile with standard targets. Run `make help` to see all available commands.

### Build and Test

```bash
make build      # Compile the project
make test       # Run tests
make lint       # Check code formatting
make format     # Format code with scalafmt
make clean      # Remove build artifacts
```

### Using SBT Directly

```bash
sbt compile
sbt test
sbt scalafmtAll
sbt scalafmtCheckAll
```