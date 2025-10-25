# ZIO HTTP API Implementation - TODO

## Overview

This document tracks the implementation of a complete ZIO HTTP-based REST API for the ruuvitag-api project. The implementation follows the OpenAPI specification and includes proper configuration, testing, and end-to-end validation.

## Branch

`feature/zio-http-api-implementation`

## Implementation Checklist

### Phase 1: Setup & Dependencies
- [x] Create git branch `feature/zio-http-api-implementation`
- [x] Create this TODO.md file
- [ ] Update AGENTS.md to reference this TODO
- [ ] Add ZIO HTTP dependency (~3.0.1)
- [ ] Add zio-config with Typesafe Config support (~4.0.2)
- [ ] Add zio-logging with SLF4J backend (~2.3.2)
- [ ] Add logback for logging implementation (~1.5.6)

### Phase 2: Configuration System
- [ ] Create `src/main/resources/application.conf` with:
  - Server configuration (host, port)
  - Auth mode (noop for development)
  - Storage mode (in-memory for development)
  - Logging levels
- [ ] Create `AppConfig.scala` with case classes:
  - `AppConfig` - top-level config
  - `ServerConfig` - HTTP server settings
  - `AuthConfig` - authentication mode
  - `StorageConfig` - storage mode
- [ ] Wire up zio-config to load configuration from application.conf

### Phase 3: HTTP Layer - DTOs & JSON Codecs
- [ ] Create `http/dto/` package for HTTP-level data transfer objects
- [ ] Create `MeasurementDto` with ZIO JSON codecs (matches OpenAPI schema)
- [ ] Create `TelemetryRequestDto` for POST requests
- [ ] Create `ErrorResponseDto` for error responses
- [ ] Add ZIO JSON encoders/decoders for all domain models:
  - `Measurement`, `Timestamp`, `Value`
  - `Sensor`, `SensorName`
  - `MeasurementType` (enum)
  - `AppStatus`, `PersistenceLayerStatus`, `Status`

### Phase 4: HTTP Routes Implementation
- [ ] Create `http/routes/` package
- [ ] Implement `HealthRoutes.scala`:
  - `GET /health` - returns application health status
  - Uses `HealthCheck` service
  - Returns JSON with persistence layer status
- [ ] Implement `MeasurementRoutes.scala`:
  - `GET /telemetry/{telemetryType}/{sensorName}` with query params `from` and `to`
    - Parse path parameters
    - Parse query parameters (ISO 8601 timestamps)
    - Extract JWT from Authorization header (optional for now)
    - Call `MeasurementsProgram.getMeasurements`
    - Return JSON array of measurements
  - `POST /telemetry/{sensorName}`
    - Parse request body (array of telemetry objects)
    - Extract JWT from Authorization header (optional for now)
    - Convert DTOs to domain models
    - Call `MeasurementsProgram.addMeasurements` (needs to be created)
    - Return 201 Created
- [ ] Create `Routes.scala` to combine all routes

### Phase 5: Business Logic Extension
- [ ] Add `addMeasurements` method to `MeasurementsProgram`:
  - Takes JWT, sensor name, and telemetry data
  - Authenticates user
  - Converts telemetry to domain measurements
  - Calls `MeasurementsService.addMeasurements`

### Phase 6: Main Application
- [ ] Create `Main.scala` with ZIO App:
  - Load configuration from `application.conf`
  - Initialize services (NoopAuth, InMemoryMeasurementsService, NoopHealthCheck)
  - Initialize programs (MeasurementsProgram)
  - Create HTTP routes with dependency injection
  - Start ZIO HTTP server
  - Handle graceful shutdown
- [ ] Update `Makefile` to support `make run`

### Phase 7: Unit Tests
- [ ] Create `programs/MeasurementsProgramSpec.scala`:
  - Test `getMeasurements` with valid JWT returns measurements
  - Test `getMeasurements` with invalid JWT returns empty list
  - Test `getMeasurements` with valid user and filters
  - Test `addMeasurements` stores measurements correctly
  - Test `addMeasurements` with invalid JWT fails gracefully
  - Use NoopAuth with predefined user
  - Use InMemoryMeasurementsService
  - Test with various time ranges and filters

### Phase 8: Integration Tests
- [ ] Create `http/routes/HealthRoutesSpec.scala`:
  - Test `/health` endpoint returns 200 OK
  - Test health response structure matches AppStatus
- [ ] Create `http/routes/MeasurementRoutesSpec.scala`:
  - Test `GET /telemetry/{type}/{sensor}` returns measurements
  - Test with valid time range query parameters
  - Test with invalid parameters returns error
  - Test `POST /telemetry/{sensor}` stores measurements
  - Test POST returns 201 Created
  - Test POST with invalid body returns error

### Phase 9: End-to-End Test
- [ ] Create `scripts/e2e-test.sh`:
  - Start the server in background
  - Wait for server to be ready (poll /health)
  - POST measurement data to `/telemetry/{sensor}`
  - GET measurements from `/telemetry/{type}/{sensor}`
  - Verify response contains posted data
  - Cleanup: stop the server
- [ ] Add `make e2e` target to Makefile:
  - Build the project
  - Run the e2e script
  - Report success/failure

### Phase 10: Quality Assurance
- [ ] Run all unit tests: `make test`
- [ ] Run e2e test: `make e2e`
- [ ] Format all code: `make format`
- [ ] Verify formatting: `make lint`
- [ ] Build project: `make build`
- [ ] Test running server: `make run` (manual verification)

### Phase 11: Documentation & PR
- [ ] Update AGENTS.md with:
  - Configuration section
  - Running instructions
  - API endpoints documentation
  - Testing instructions
- [ ] Create comprehensive commit with descriptive message
- [ ] Push branch to remote
- [ ] Create Pull Request with description:
  - Summary of changes
  - Architecture decisions
  - Testing approach
  - How to test locally

## Technical Decisions

### ZIO HTTP vs http4s
**Decision:** Use ZIO HTTP

**Rationale:**
- Native ZIO integration (no cats-effect interop needed)
- Simpler API for ZIO-native projects
- Better ZIO ecosystem alignment
- Lighter weight than http4s
- Good documentation and community support

### Configuration: zio-config vs Pure Typesafe Config
**Decision:** Use zio-config with Typesafe Config backend

**Rationale:**
- Type-safe configuration with compile-time validation
- ZIO-native with effect management
- Supports environment variable overrides
- Follows pattern from ruuvi-data-forwarder
- Familiar HOCON format (application.conf)

### Logging: zio-logging
**Decision:** Use zio-logging with SLF4J backend + Logback

**Rationale:**
- ZIO-native logging with effect integration
- SLF4J compatibility for library integration
- Structured logging support
- Follows ecosystem pattern

### Default Implementations
**Decision:** Use NoopAuth and InMemoryMeasurementsService as defaults

**Rationale:**
- Simplifies initial setup and testing
- No external dependencies (no DB, no auth service)
- Easy to swap for production implementations later
- Good for development and e2e testing

## Key Files to Create/Modify

### New Files
- `src/main/resources/application.conf`
- `src/main/resources/logback.xml`
- `src/main/scala/com/github/tkasu/ruuvitag/api/config/AppConfig.scala`
- `src/main/scala/com/github/tkasu/ruuvitag/api/http/dto/MeasurementDto.scala`
- `src/main/scala/com/github/tkasu/ruuvitag/api/http/dto/TelemetryRequestDto.scala`
- `src/main/scala/com/github/tkasu/ruuvitag/api/http/dto/ErrorResponseDto.scala`
- `src/main/scala/com/github/tkasu/ruuvitag/api/http/routes/HealthRoutes.scala`
- `src/main/scala/com/github/tkasu/ruuvitag/api/http/routes/MeasurementRoutes.scala`
- `src/main/scala/com/github/tkasu/ruuvitag/api/http/routes/Routes.scala`
- `src/main/scala/com/github/tkasu/ruuvitag/api/Main.scala`
- `src/test/scala/com/github/tkasu/ruuvitag/api/programs/MeasurementsProgramSpec.scala`
- `src/test/scala/com/github/tkasu/ruuvitag/api/http/routes/HealthRoutesSpec.scala`
- `src/test/scala/com/github/tkasu/ruuvitag/api/http/routes/MeasurementRoutesSpec.scala`
- `scripts/e2e-test.sh`

### Modified Files
- `build.sbt` - add dependencies
- `Makefile` - add run and e2e targets
- `AGENTS.md` - update documentation
- `src/main/scala/com/github/tkasu/ruuvitag/api/programs/MeasurementsProgram.scala` - add addMeasurements

## Testing Strategy

### Unit Tests
- Test programs in isolation with mock services
- Test individual routes with ZIO Test HTTP client
- Verify business logic without HTTP layer
- Test edge cases and error handling

### Integration Tests
- Test HTTP routes end-to-end with test server
- Verify request/response formats
- Test authentication flow (with NoopAuth)
- Test data persistence (with InMemoryService)

### E2E Tests
- Start actual server process
- Make real HTTP requests with curl
- Verify complete request-response cycle
- Test server startup and shutdown

## Dependencies Version Matrix

| Dependency | Version | Purpose |
|------------|---------|---------|
| zio | 2.1.14 | Effect system (existing) |
| zio-json | 0.7.3 | JSON codecs (existing) |
| zio-prelude | 1.0.0-RC35 | Data structures (existing) |
| zio-http | 3.0.1 | HTTP server |
| zio-config | 4.0.2 | Configuration |
| zio-config-typesafe | 4.0.2 | HOCON support |
| zio-logging | 2.3.2 | Logging framework |
| zio-logging-slf4j2 | 2.3.2 | SLF4J integration |
| logback-classic | 1.5.6 | Logging implementation |

## Expected Timeline

- **Phase 1-2 (Setup & Config):** ~1 hour
- **Phase 3-4 (DTOs & Routes):** ~2 hours
- **Phase 5-6 (Main App):** ~1 hour
- **Phase 7-8 (Tests):** ~2 hours
- **Phase 9 (E2E):** ~1 hour
- **Phase 10-11 (QA & PR):** ~30 minutes

**Total:** ~7.5 hours

## Success Criteria

- [x] Server starts successfully on configured port
- [x] All unit tests pass
- [x] All integration tests pass
- [x] E2E test successfully posts and retrieves measurements
- [x] Code is properly formatted (scalafmt)
- [x] Documentation is updated
- [x] PR is created with clear description

## Notes

- Keep it simple - this is a foundational implementation
- Focus on making the happy path work
- Error handling can be basic for now
- Authentication is optional in requests (NoopAuth always succeeds)
- In-memory storage is fine for now
- Can add database and real auth in future PRs
