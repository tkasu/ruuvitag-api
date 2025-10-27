package com.github.tkasu.ruuvitag.api.services

import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource
import zio.*
import zio.test.*
import zio.prelude.NonEmptyList
import com.github.tkasu.ruuvitag.api.domain.measurement.*
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType
import com.github.tkasu.ruuvitag.api.domain.sensor.{Sensor, MacAddress}
import com.github.tkasu.ruuvitag.api.domain.user.{User, UserId, UserName}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import java.nio.file.{Files, Paths}

/** Integration tests for SqliteMeasurementsService.
  *
  * These tests use a temporary SQLite database file that is cleaned up after
  * each test. The test database is separate from the development database to
  * avoid interfering with local development data.
  */
object SqliteMeasurementsServiceSpec extends ZIOSpecDefault:

  val testUser1 = User(UserId(UUID.randomUUID()), UserName("user1"))
  val testUser2 = User(UserId(UUID.randomUUID()), UserName("user2"))
  val sensor1 = Sensor(MacAddress("FE:26:88:7A:66:66"))
  val sensor2 = Sensor(MacAddress("D5:12:34:66:14:14"))

  // Use UTC and millisecond precision to match database storage
  val baseTime = OffsetDateTime.now(java.time.ZoneOffset.UTC).withNano(0)

  val tempMeasurement1 = Measurement(
    sensor = sensor1,
    measurementType = MeasurementType.Temperature,
    timestamp = Timestamp(baseTime),
    value = Value(22.5)
  )

  val tempMeasurement2 = Measurement(
    sensor = sensor1,
    measurementType = MeasurementType.Temperature,
    timestamp = Timestamp(baseTime.plusHours(1)),
    value = Value(23.0)
  )

  val humidityMeasurement = Measurement(
    sensor = sensor1,
    measurementType = MeasurementType.Humidity,
    timestamp = Timestamp(baseTime.plusHours(2)),
    value = Value(45.5)
  )

  val sensor2Measurement = Measurement(
    sensor = sensor2,
    measurementType = MeasurementType.Temperature,
    timestamp = Timestamp(baseTime.plusHours(1)),
    value = Value(20.0)
  )

  // Create a test database with a unique name for each test run
  // Uses system temp directory to work in any environment (CI, local, etc.)
  private def createTestDataSource: ZIO[Scope, Throwable, DataSource] =
    ZIO
      .acquireRelease(
        ZIO.attempt {
          // Use system temp directory for test databases
          val testDbFile =
            Files.createTempFile("ruuvitag-test-", ".db")
          val testDbPath = testDbFile.toString
          val hikariConfig = new HikariConfig()
          hikariConfig.setJdbcUrl(s"jdbc:sqlite:$testDbPath")
          hikariConfig.setDriverClassName("org.sqlite.JDBC")
          hikariConfig.setMaximumPoolSize(5)
          hikariConfig.setConnectionTimeout(30000)
          val dataSource = new HikariDataSource(hikariConfig)
          (dataSource, testDbPath)
        }
      ) { case (dataSource, testDbPath) =>
        ZIO.attempt {
          dataSource.close()
          // Clean up test database file
          val dbFile = Paths.get(testDbPath)
          val journalFile = Paths.get(s"$testDbPath-journal")
          Files.deleteIfExists(dbFile)
          Files.deleteIfExists(journalFile)
        }.orDie
      }
      .map(_._1)

  private def createTestService
      : ZIO[Scope, Throwable, SqliteMeasurementsService] =
    for
      dataSource <- createTestDataSource
      _ <- SqliteMeasurementsService.initSchema(dataSource)
      service = SqliteMeasurementsService(
        io.getquill.jdbczio.Quill.Sqlite(io.getquill.SnakeCase, dataSource)
      )
    yield service

  def spec = suite("SqliteMeasurementsServiceSpec")(
    test("should add and retrieve measurements for a user") {
      ZIO.scoped {
        for
          service <- createTestService
          _ <- service.addMeasurements(
            testUser1,
            NonEmptyList(tempMeasurement1, tempMeasurement2)
          )
          measurements <- service.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Temperature,
            baseTime.minusHours(1),
            baseTime.plusHours(2)
          )
        yield assertTrue(
          measurements.length == 2,
          measurements.contains(tempMeasurement1),
          measurements.contains(tempMeasurement2)
        )
      }
    },
    test("should filter measurements by MAC address") {
      ZIO.scoped {
        for
          service <- createTestService
          _ <- service.addMeasurements(
            testUser1,
            NonEmptyList(tempMeasurement1, sensor2Measurement)
          )
          sensor1Measurements <- service.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Temperature,
            baseTime.minusHours(1),
            baseTime.plusHours(2)
          )
          sensor2Measurements <- service.getMeasurements(
            testUser1,
            sensor2.macAddress,
            MeasurementType.Temperature,
            baseTime.minusHours(1),
            baseTime.plusHours(2)
          )
        yield assertTrue(
          sensor1Measurements.length == 1,
          sensor1Measurements.head == tempMeasurement1,
          sensor2Measurements.length == 1,
          sensor2Measurements.head == sensor2Measurement
        )
      }
    },
    test("should filter measurements by measurement type") {
      ZIO.scoped {
        for
          service <- createTestService
          _ <- service.addMeasurements(
            testUser1,
            NonEmptyList(tempMeasurement1, humidityMeasurement)
          )
          tempMeasurements <- service.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Temperature,
            baseTime.minusHours(1),
            baseTime.plusHours(3)
          )
          humidityMeasurements <- service.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Humidity,
            baseTime.minusHours(1),
            baseTime.plusHours(3)
          )
        yield assertTrue(
          tempMeasurements.length == 1,
          tempMeasurements.head == tempMeasurement1,
          humidityMeasurements.length == 1,
          humidityMeasurements.head == humidityMeasurement
        )
      }
    },
    test("should filter measurements by time range") {
      ZIO.scoped {
        for
          service <- createTestService
          _ <- service.addMeasurements(
            testUser1,
            NonEmptyList(
              tempMeasurement1,
              tempMeasurement2,
              humidityMeasurement
            )
          )
          // Query only the first hour
          earlyMeasurements <- service.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Temperature,
            baseTime.minusHours(1),
            baseTime.plusMinutes(30)
          )
          // Query from 1 hour onwards
          laterMeasurements <- service.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Temperature,
            baseTime.plusMinutes(30),
            baseTime.plusHours(3)
          )
        yield assertTrue(
          earlyMeasurements.length == 1,
          earlyMeasurements.head == tempMeasurement1,
          laterMeasurements.length == 1,
          laterMeasurements.head == tempMeasurement2
        )
      }
    },
    test("should isolate measurements by user") {
      ZIO.scoped {
        for
          service <- createTestService
          _ <- service.addMeasurements(
            testUser1,
            NonEmptyList(tempMeasurement1)
          )
          _ <- service.addMeasurements(
            testUser2,
            NonEmptyList(tempMeasurement2)
          )
          user1Measurements <- service.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Temperature,
            baseTime.minusHours(1),
            baseTime.plusHours(2)
          )
          user2Measurements <- service.getMeasurements(
            testUser2,
            sensor1.macAddress,
            MeasurementType.Temperature,
            baseTime.minusHours(1),
            baseTime.plusHours(2)
          )
        yield assertTrue(
          user1Measurements.length == 1,
          user1Measurements.head == tempMeasurement1,
          user2Measurements.length == 1,
          user2Measurements.head == tempMeasurement2
        )
      }
    },
    test("should return empty list when no measurements match filters") {
      ZIO.scoped {
        for
          service <- createTestService
          _ <- service.addMeasurements(
            testUser1,
            NonEmptyList(tempMeasurement1)
          )
          measurements <- service.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Pressure, // Different type
            baseTime.minusHours(1),
            baseTime.plusHours(2)
          )
        yield assertTrue(measurements.isEmpty)
      }
    },
    test("should handle multiple batches of measurements") {
      ZIO.scoped {
        for
          service <- createTestService
          _ <- service.addMeasurements(
            testUser1,
            NonEmptyList(tempMeasurement1)
          )
          _ <- service.addMeasurements(
            testUser1,
            NonEmptyList(tempMeasurement2)
          )
          measurements <- service.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Temperature,
            baseTime.minusHours(1),
            baseTime.plusHours(2)
          )
        yield assertTrue(
          measurements.length == 2,
          measurements.contains(tempMeasurement1),
          measurements.contains(tempMeasurement2)
        )
      }
    },
    test("should handle exact boundary matching (inclusive time range)") {
      ZIO.scoped {
        for
          service <- createTestService
          _ <- service.addMeasurements(
            testUser1,
            NonEmptyList(tempMeasurement1, tempMeasurement2)
          )
          // Query with exact timestamp boundaries
          measurements <- service.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Temperature,
            baseTime, // Exact match on 'from'
            baseTime.plusHours(1) // Exact match on 'to'
          )
        yield assertTrue(
          measurements.length == 2,
          measurements.contains(tempMeasurement1),
          measurements.contains(tempMeasurement2)
        )
      }
    },
    test("should return empty list when querying empty database") {
      ZIO.scoped {
        for
          service <- createTestService
          measurements <- service.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Temperature,
            baseTime.minusHours(1),
            baseTime.plusHours(2)
          )
        yield assertTrue(measurements.isEmpty)
      }
    },
    test("should persist data across service instances") {
      // This test creates two service instances with the same database
      // to verify data persistence
      ZIO.scoped {
        for
          dataSource <- createTestDataSource
          _ <- SqliteMeasurementsService.initSchema(dataSource)

          // Create first service instance and add data
          service1 = SqliteMeasurementsService(
            io.getquill.jdbczio.Quill.Sqlite(io.getquill.SnakeCase, dataSource)
          )
          _ <- service1.addMeasurements(
            testUser1,
            NonEmptyList(tempMeasurement1)
          )

          // Create second service instance and read data
          service2 = SqliteMeasurementsService(
            io.getquill.jdbczio.Quill.Sqlite(io.getquill.SnakeCase, dataSource)
          )
          measurements <- service2.getMeasurements(
            testUser1,
            sensor1.macAddress,
            MeasurementType.Temperature,
            baseTime.minusHours(1),
            baseTime.plusHours(2)
          )
        yield assertTrue(
          measurements.length == 1,
          measurements.head == tempMeasurement1
        )
      }
    }
  )
