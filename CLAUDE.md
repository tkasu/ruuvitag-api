# ruuvitag-api

## Overview

**ruuvitag-api** is a REST API server built with Scala 3 and ZIO for persisting and serving sensor measurements from Ruuvi Tags. This is a modern, functional-first redesign representing the API layer of the Ruuvitag telemetry ecosystem.

**Status:** Work in Progress - Foundation/Skeleton (domain models and service interfaces defined)

**Language:** Scala 3.3.1

**Effect System:** ZIO 2.0.13

**Repository:** https://github.com/tkasu/ruuvitag-api

**License:** MIT (Copyright 2023 Tomi Kasurinen)

## Purpose

This application serves as the HTTP API and persistence layer by:
- Providing REST endpoints for storing telemetry data
- Querying sensor measurements by sensor, type, and time range
- Supporting user authentication and authorization (JWT-based)
- Serving as the data access point for web clients, dashboards, and mobile apps
- (Future) Real-time streaming via WebSockets
- (Future) Aggregated analytics and metrics

## Architecture

### Tech Stack

- **Language:** Scala 3.3.1 (modern functional language with enhanced syntax)
- **Effect System:** ZIO 2.0.13 (functional effects for async/concurrent operations)
- **JSON:** ZIO JSON 0.6.1 (compile-time codec derivation, zero runtime reflection)
- **Utilities:** ZIO Prelude 1.0.0-RC20 (functional data structures)
- **Build Tool:** SBT 1.8.2
- **Testing:** ZIO Test 2.0.13 (effect-based property testing)
- **Code Quality:** Scalafmt 3.7.4, Scalafix 0.10.4

### Design Philosophy

**Tagless Final / Hexagonal Architecture:**
- Domain models are pure (no effects, no logic)
- Services defined as traits (algebras) - contracts without implementation
- Programs orchestrate services to implement business logic
- Clear separation enables multiple implementations and testability

**Type Safety:**
- Value objects for semantic types (SensorName, UserId, etc.)
- Scala 3 enums for restricted values (MeasurementType)
- Non-empty collections enforced at type level
- ZIO's error channel for type-safe error handling

**Functional Programming:**
- Pure functions throughout
- Effects wrapped in ZIO Task
- Composable business logic with for-comprehensions
- Resource safety with automatic cleanup

### Project Structure

```
ruuvitag-api/
├── build.sbt                           # Build configuration
├── README.md                           # Project description
├── LICENSE.md                          # MIT License
├── .scalafmt.conf                      # Code formatting rules
├── .gitignore                          # Git exclusions
│
├── project/
│   ├── build.properties                # SBT version 1.8.2
│   └── plugins.sbt                     # sbt-scalafix, sbt-scalafmt
│
├── src/
│   ├── main/scala/com/github/tkasu/ruuvitag/api/
│   │   ├── domain/                     # Domain models (9 files)
│   │   │   ├── measurement.scala       # Measurement, Timestamp, Value
│   │   │   ├── sensor.scala            # Sensor, SensorName
│   │   │   ├── user.scala              # User, UserId, UserName
│   │   │   ├── measurementtype.scala   # Temperature, Humidity, Pressure enum
│   │   │   └── healthcheck.scala       # AppStatus, PersistenceLayerStatus
│   │   │
│   │   ├── services/                   # Service traits (3 files)
│   │   │   ├── Auth.scala              # Authentication algebra
│   │   │   ├── HealthCheck.scala       # Health check algebra
│   │   │   └── MeasurementsService.scala # Data access algebra
│   │   │
│   │   └── programs/                   # Business logic (1 file)
│   │       └── MeasurementsProgram.scala # Orchestration logic
│   │
│   └── test/scala/com/github/tkasu/ruuvitag/api/
│       └── (empty - tests to be written)
│
└── src/main/resources/
    └── openapi.yaml                    # OpenAPI 3.1.0 specification
```

**Lines of Code:** ~150 LOC (minimal foundation)

### Key Components

#### Domain Models (`domain/*.scala`)

**measurement.scala** - Core telemetry model
```scala
case class Timestamp(value: OffsetDateTime)
case class Value(value: Double)

case class Measurement(
    sensor: Sensor,
    measurementType: MeasurementType,
    timestamp: Timestamp,
    value: Value
)
```

**sensor.scala** - Sensor identity
```scala
case class SensorName(value: String)
case class Sensor(name: SensorName)
```

**measurementtype.scala** - Measurement categories
```scala
enum MeasurementType:
  case Temperature, Pressure, Humidity
```

**user.scala** - User identity and authorization
```scala
case class UserId(value: UUID)
case class UserName(value: String)
case class User(id: UserId, name: UserName)
```

**healthcheck.scala** - Application health status
```scala
enum Status:
  case Ok, Unreachable

case class PersistenceLayerStatus(value: Status)
case class AppStatus(persistenceLayerStatus: PersistenceLayerStatus)

// Includes ZIO JSON encoders for Status and AppStatus
given encoder: JsonEncoder[Status] = DeriveJsonEncoder.gen[Status]
```

#### Service Algebras (`services/*.scala`)

**Auth.scala** - Authentication contract
```scala
trait Auth:
  def findUser(token: String): Task[Option[User]]
```
- Takes JWT/auth token string
- Returns User wrapped in ZIO Task
- Option indicates user found or not

**MeasurementsService.scala** - Data access contract
```scala
trait MeasurementsService:
  def getMeasurements(
      user: User,
      sensorName: SensorName,
      measurementType: MeasurementType,
      from: OffsetDateTime,
      to: OffsetDateTime
  ): Task[List[Measurement]]

  def addMeasurements(
      user: User,
      measurements: NonEmptyList[Measurement]
  ): Task[Unit]
```
- Query measurements by sensor, type, and time range
- Insert batches (NonEmptyList ensures non-empty)
- User-scoped for multi-tenant support

**HealthCheck.scala** - Health status contract
```scala
trait HealthCheck:
  def status(): Task[AppStatus]
```
- Returns application health status
- Checks persistence layer health

#### Business Logic (`programs/MeasurementsProgram.scala`)

**MeasurementsProgram** - Orchestrates auth + data access
```scala
final case class MeasurementsProgram(
    auth: Auth,
    measurementsService: MeasurementsService
):
  def getMeasurements(
      userJwt: String,
      sensorName: SensorName,
      measurementType: MeasurementType,
      from: OffsetDateTime,
      to: OffsetDateTime
  ): Task[List[Measurement]] = for
    maybeUser <- auth.findUser(userJwt)
    measurements <- maybeUser
      .map(user =>
        measurementsService
          .getMeasurements(user, sensorName, measurementType, from, to)
      )
      .getOrElse(ZIO.succeed(List.empty))
  yield measurements
```

**Logic:**
1. Authenticates user from JWT token
2. If authenticated, queries measurements from service
3. If not authenticated, returns empty list (fails gracefully)
4. Uses for-comprehension for clean effect composition

#### API Specification (`src/main/resources/openapi.yaml`)

**OpenAPI 3.1.0 specification** defines HTTP interface:

**GET /telemetry/{telemetryType}/{sensorName}**
- Returns array of measurements
- Query params: from, to (time range)
- Response: 200 OK with Measurement array

**POST /telemetry/{sensorName}**
- Sends new telemetry data
- Request body: Array of telemetry objects
- Response: 201 Created

**Schemas:**
- Measurement: sensor_name, timestamp (unix ms), value

**Status:** Specification exists but HTTP implementation missing

## Building and Running

### Prerequisites

1. **Java Development Kit (JDK)**
   - JDK 11 or higher
   - Tested with JDK 20
   ```bash
   java -version
   ```

2. **SBT (Scala Build Tool)**
   ```bash
   # macOS
   brew install sbt

   # Linux
   echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
   curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
   sudo apt-get update
   sudo apt-get install sbt
   ```

### Build Commands

This project includes a Makefile with standard targets. Run `make help` to see all available commands.

```bash
# Using Make (recommended)
make build    # Compile project
make test     # Run all tests
make lint     # Check code formatting
make format   # Format code with scalafmt
make clean    # Remove build artifacts
make run      # Run application (when implemented)
make console  # Start SBT console

# Or use SBT directly
sbt compile
sbt test
sbt scalafmtCheckAll
sbt scalafmtAll

# Package into JAR
sbt package
# Output: target/scala-3.3.1/ruuvitag-api_3-0.1.0-SNAPSHOT.jar
```

### Running

**Current Status:** Cannot run - no main entry point or HTTP server

**Why:**
- No `Main` object with entry point
- No HTTP server dependency (no http4s, Finch, etc.)
- No service implementations (Auth and MeasurementsService are just traits)

**To make runnable, need to:**
1. Add HTTP server dependency (http4s recommended for ZIO)
2. Implement service algebras with real logic
3. Create HTTP routes/handlers
4. Add Main object that starts server

### Testing

```bash
# Using Make (recommended)
make test     # Run all tests
make lint     # Check code formatting

# Or use SBT directly
sbt test
sbt scalafmtCheckAll test
```

**Current Status:** No tests written yet

Test directory exists but is empty:
```
src/test/scala/com/github/tkasu/ruuvitag/api/
```

**ZIO Test Framework Ready:**
- `zio-test` and `zio-test-sbt` dependencies configured
- Effect-based property testing
- Layer testing for service implementations

**Example test (to be written):**
```scala
import zio.test.*

object MeasurementsProgramSpec extends ZIOSpecDefault:
  def spec = suite("MeasurementsProgram")(
    test("returns empty list for invalid token") {
      for
        program <- ZIO.succeed(MeasurementsProgram(MockAuth, MockService))
        result <- program.getMeasurements("invalid-token", ...)
      yield assertTrue(result.isEmpty)
    }
  )
```

## Configuration

**Current State:** Zero configuration - everything hardcoded

**What's Missing:**
- Database connection settings
- Authentication secrets/JWT keys
- HTTP server port
- Logging configuration

**Future Configuration (Typesafe Config):**
```hocon
# application.conf
ruuvitag-api {
  server {
    host = "0.0.0.0"
    port = 8081
  }

  database {
    url = "jdbc:postgresql://localhost:5432/ruuvi_telemetry"
    user = "postgres"
    password = ${?DB_PASSWORD}
    pool-size = 10
  }

  auth {
    jwt-secret = ${JWT_SECRET}
    token-expiry = "24h"
  }

  cors {
    allowed-origins = ["http://localhost:3000"]
  }
}
```

## Integration

### Position in Ecosystem

```
ruuvi-reader-rs (Rust BLE Scanner)
        ↓ stdout (JSON)
ruuvi-data-forwarder (Scala 3 + ZIO Streams)
        ↓ HTTP POST
ruuvitag-api (Scala 3 + ZIO) ← YOU ARE HERE
        ↓ HTTP REST API
Web Clients / Dashboards / Mobile Apps
```

### From ruuvi-data-forwarder (Future)

**HTTP Sink in Forwarder:**
```bash
ruuvi-reader-rs | \
  java -jar ruuvi-data-forwarder-assembly.jar \
    --sink=http \
    --url=http://localhost:8081/telemetry
```

**POST Endpoint:**
```
POST /telemetry/{sensorName}
Content-Type: application/json
Authorization: Bearer <jwt-token>

[
  {
    "telemetry_type": "temperature",
    "data": [22.5, 22.6, 22.7]
  },
  {
    "telemetry_type": "humidity",
    "data": [45.2, 45.3, 45.1]
  }
]
```

### To Web Clients (Future)

**Query Endpoint:**
```bash
GET /telemetry/temperature/sensor-name?from=2024-01-01T00:00:00Z&to=2024-01-31T23:59:59Z
Authorization: Bearer <jwt-token>

Response:
[
  {
    "sensor": {"name": "sensor-1"},
    "measurementType": "Temperature",
    "timestamp": "2024-01-15T10:30:00Z",
    "value": 22.5
  },
  ...
]
```

## Development

### Git History

Recent commits show architectural evolution:

```
699a297 - Migrate project template to zio (Latest)
9fa3cf9 - Add program template
64ba3f5 - Update README
fffe704 - Add Auth and Healthcheck Algebras
77c2458 - Add initial domain definitions
52dc892 - Add LICENSE.md
d12dd96 - Project template and initial openapi
```

**Development Phase:** Early foundation, structure being established

### Code Quality

**Scalafmt (3.7.4):**
```bash
# Check formatting
sbt scalafmtCheckAll

# Auto-format
sbt scalafmtAll
```

**Scalafix (0.10.4):**
- Automated code rewriting
- Pattern matching improvements
- Deprecation handling

**Configuration:** `.scalafmt.conf` with Scala 3 dialect, 2-space indentation

### Adding HTTP Server (Next Step)

**Recommended: http4s + ZIO**

**Add to build.sbt:**
```scala
val http4sVersion = "0.23.23"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion
)
```

**Create HTTP routes:**
```scala
import org.http4s.*
import org.http4s.dsl.io.*
import zio.interop.catz.*

object Routes:
  def measurementRoutes(program: MeasurementsProgram): HttpRoutes[Task] =
    HttpRoutes.of[Task] {
      case GET -> Root / "telemetry" / telemetryType / sensorName :?
          FromParam(from) +& ToParam(to) =>
        // Extract JWT from Authorization header
        // Call program.getMeasurements(...)
        // Return JSON response
        Ok(...)
    }
```

### Adding Database (Next Step)

**Recommended: Doobie + PostgreSQL**

**Add to build.sbt:**
```scala
val doobieVersion = "1.0.0-RC4"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion
)
```

**Implement MeasurementsService:**
```scala
import doobie.*
import doobie.implicits.*

class PostgresMeasurementsService(xa: Transactor[Task])
    extends MeasurementsService:

  def getMeasurements(
      user: User,
      sensorName: SensorName,
      measurementType: MeasurementType,
      from: OffsetDateTime,
      to: OffsetDateTime
  ): Task[List[Measurement]] =
    sql"""
      SELECT sensor_name, measurement_type, timestamp, value
      FROM measurements
      WHERE sensor_name = ${sensorName.value}
        AND measurement_type = ${measurementType.toString}
        AND timestamp BETWEEN $from AND $to
        AND user_id = ${user.id.value}
      ORDER BY timestamp DESC
    """.query[Measurement]
      .to[List]
      .transact(xa)
```

**Database Schema:**
```sql
CREATE TABLE measurements (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID NOT NULL,
  sensor_name VARCHAR(255) NOT NULL,
  measurement_type VARCHAR(50) NOT NULL,
  timestamp TIMESTAMPTZ NOT NULL,
  value DOUBLE PRECISION NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_measurements_user_sensor ON measurements(user_id, sensor_name);
CREATE INDEX idx_measurements_timestamp ON measurements(timestamp DESC);
```

### Adding Authentication (Next Step)

**JWT Library:**
```scala
libraryDependencies += "com.github.jwt-scala" %% "jwt-zio-json" % "9.4.4"
```

**Implement Auth:**
```scala
import pdi.jwt.*

class JwtAuth(secret: String) extends Auth:
  def findUser(token: String): Task[Option[User]] =
    ZIO.attempt {
      JwtZIOJson.decode(token, secret, JwtAlgorithm.HS256)
    }.map {
      case Success(claim) =>
        // Extract user from claim
        Some(User(...))
      case Failure(_) =>
        None
    }
```

## Dependencies

Key dependencies from build.sbt:

| Dependency | Version | Purpose |
|------------|---------|---------|
| zio | 2.0.13 | Effect system and async runtime |
| zio-json | 0.6.1 | JSON codec derivation (zero reflection) |
| zio-prelude | 1.0.0-RC20 | Functional data structures (NonEmptyList) |
| zio-test | 2.0.13 | Effect-based property testing |
| zio-test-sbt | 2.0.13 | SBT test runner |

**Notable Omissions (to be added):**
- ❌ HTTP server (http4s, Finch, etc.)
- ❌ Database driver (Doobie, Slick, etc.)
- ❌ JWT library
- ❌ Logging (zio-logging)
- ❌ Configuration (zio-config)

## Current Limitations

**What Works:**
- ✅ Type-safe domain models
- ✅ Service trait definitions
- ✅ Business logic composition
- ✅ OpenAPI specification
- ✅ Build and formatting infrastructure

**What Doesn't Work:**
- ❌ Cannot run (no main entry point)
- ❌ No HTTP endpoints (no server)
- ❌ No database persistence
- ❌ No authentication implementation
- ❌ No tests written
- ❌ No configuration system
- ❌ No logging

## Roadmap

### Phase 1: Runnable HTTP Server (Weeks 1-2)
- [ ] Add http4s dependency
- [ ] Implement HTTP routes matching OpenAPI spec
- [ ] Create Main object and server startup
- [ ] Add configuration library (zio-config or Typesafe Config)
- [ ] Basic request/response logging

### Phase 2: Data Persistence (Weeks 3-4)
- [ ] Add Doobie for database access
- [ ] Implement PostgresMeasurementsService
- [ ] Database migrations (Flyway or Liquibase)
- [ ] Connection pooling (HikariCP)
- [ ] Transaction management

### Phase 3: Authentication (Week 5)
- [ ] Add JWT library
- [ ] Implement JwtAuth service
- [ ] User registration/login endpoints
- [ ] Password hashing (BCrypt)
- [ ] Token refresh mechanism

### Phase 4: Production Ready (Week 6)
- [ ] Comprehensive tests (ZIO Test)
- [ ] Structured logging (zio-logging)
- [ ] Metrics (Prometheus)
- [ ] Health check endpoint implementation
- [ ] CORS configuration
- [ ] Docker support

### Phase 5: Advanced Features (Future)
- [ ] WebSocket streaming for real-time updates
- [ ] TimescaleDB for time-series optimization
- [ ] Data aggregation endpoints (hourly, daily averages)
- [ ] Grafana dashboards
- [ ] Rate limiting
- [ ] API versioning
- [ ] Kubernetes manifests

## Troubleshooting

**Issue: Cannot run with `sbt run`**
- Expected - no main entry point defined yet
- Need to implement HTTP server and create Main object

**Issue: Compilation errors after adding dependencies**
```bash
# Clear SBT cache
sbt clean
rm -rf ~/.ivy2/cache
sbt update compile
```

**Issue: Formatting check fails**
```bash
# Auto-fix formatting
sbt scalafmtAll
sbt scalafmtCheckAll
```

**Issue: Tests not discovered**
- Ensure test files extend `ZIOSpecDefault`
- Check that test files are in `src/test/scala/` directory
- Verify `zio-test-sbt` is in libraryDependencies

## Comparison with Old ruuvi-api

This project is a **redesign** of an earlier Scala 2 + Finch API:

| Aspect | Old ruuvi-api | New ruuvitag-api |
|--------|---------------|------------------|
| **Language** | Scala 2.13 | Scala 3.3.1 |
| **Effect System** | Cats Effect | ZIO 2.0.13 |
| **HTTP Framework** | Finch | None yet (skeleton) |
| **JSON** | Circe | ZIO JSON |
| **Architecture** | Direct endpoints | Tagless Final + Programs |
| **Tests** | ScalaTest | ZIO Test |
| **Status** | Basic skeleton | Foundation only |

**Why Redesign?**
- **Modern Scala 3:** Better syntax, improved type inference, enum support
- **ZIO over Cats Effect:** More batteries-included, better for microservices
- **Cleaner Architecture:** Hexagonal architecture with clear boundaries
- **No Framework Lock-in:** Algebras work with any HTTP framework
- **Better Composability:** ZIO's resource management and error handling

## Related Projects

- **ruuvi-reader-rs** - Rust BLE scanner feeding raw sensor data
- **ruuvi-data-forwarder** - Scala 3 + ZIO Streams middleware for data routing

## Resources

- [ZIO Documentation](https://zio.dev/)
- [ZIO JSON](https://github.com/zio/zio-json)
- [ZIO Prelude](https://github.com/zio/zio-prelude)
- [Scala 3 Book](https://docs.scala-lang.org/scala3/book/introduction.html)
- [http4s Documentation](https://http4s.org/) (for future HTTP implementation)
- [Doobie Documentation](https://tpolecat.github.io/doobie/) (for future database)
- [Project Repository](https://github.com/tkasu/ruuvitag-api)

## Contributing

This is an early-stage project. Potential contributions:

- Implement HTTP server (http4s integration)
- Implement database layer (Doobie + PostgreSQL)
- Implement JWT authentication
- Write comprehensive tests
- Add configuration system
- Add logging and metrics
- Create Docker support
- Write API documentation

## License

MIT License - Copyright 2023 Tomi Kasurinen

See LICENSE.md for full text.
