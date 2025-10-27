# Ruuvitag API

WIP Program to persist and get measurements from Ruuvi tags.

## Prerequisites

- **Java 25 LTS** (Eclipse Temurin) - Install via sdkman: `sdk install java 25.0.0-tem`
- **SBT 1.11.7** (specified in project/build.properties)
- **Scala 3.7.3** (managed by SBT)

## Running the Application

### Quick Start

```bash
# Run with default configuration (localhost:8081)
make run

# Or using SBT directly
sbt run
```

The server will start on `http://0.0.0.0:8081` by default.

### Available Endpoints

- `GET /health` - Health check endpoint
- `GET /metrics` - Prometheus metrics endpoint
- `GET /telemetry/{measurementType}?macAddress={macAddress}&from={timestamp}&to={timestamp}` - Get measurements
- `POST /telemetry` - Add measurements

### Example Usage

```bash
# Check health
curl http://localhost:8081/health

# Get Prometheus metrics
curl http://localhost:8081/metrics

# Add measurements
curl -X POST http://localhost:8081/telemetry \
  -H "Content-Type: application/json" \
  -d '[
    {
      "telemetry_type": "temperature",
      "data": [
        {
          "mac_address": "FE:26:88:7A:66:66",
          "timestamp": 1736942400000,
          "value": 22.5
        }
      ]
    }
  ]'

# Get temperature measurements (timestamps in ISO 8601 format or milliseconds since epoch)
curl "http://localhost:8081/telemetry/Temperature?macAddress=FE:26:88:7A:66:66&from=2025-01-01T00:00:00Z&to=2025-01-31T23:59:59Z"
```

## Configuration

The application uses HOCON configuration format. Default configuration is in `src/main/resources/application.conf`.

### Default Configuration

```hocon
ruuvitag-api {
  server {
    host = "0.0.0.0"      # Bind to all interfaces
    port = 8081           # Default port
  }

  auth {
    mode = "noop"         # No authentication (development mode)
  }

  storage {
    mode = "in-memory"    # In-memory storage (development mode)
  }
}
```

### Environment Variables

Configuration can be overridden using environment variables:

```bash
# Change server host and port
SERVER_HOST=localhost SERVER_PORT=9000 make run

# Or with SBT
SERVER_HOST=localhost SERVER_PORT=9000 sbt run
```

Available environment variables:
- `SERVER_HOST` - Override server host (default: `0.0.0.0`)
- `SERVER_PORT` - Override server port (default: `8081`)
- `AUTH_MODE` - Authentication mode (default: `noop`)
- `STORAGE_MODE` - Storage mode (default: `in-memory`)

### Configuration Modes

**Authentication Modes:**
- `noop` - No authentication (development only, all requests authenticated as default user)

**Storage Modes:**
- `in-memory` - In-memory storage (data lost on restart, for development/testing)

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