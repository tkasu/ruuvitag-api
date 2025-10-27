package com.github.tkasu.ruuvitag.api.programs

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.prelude.NonEmptyList
import java.time.OffsetDateTime
import java.util.UUID

import com.github.tkasu.ruuvitag.api.services.*
import com.github.tkasu.ruuvitag.api.domain.user.{User, UserId, UserName}
import com.github.tkasu.ruuvitag.api.domain.sensor.{Sensor, MacAddress}
import com.github.tkasu.ruuvitag.api.domain.measurement.{
  Measurement,
  Timestamp,
  Value
}
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType

object MeasurementsProgramSpec extends ZIOSpecDefault:

  val testUser = User(
    UserId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
    UserName("test-user")
  )

  val validJwt = "valid-token"
  val invalidJwt = "invalid-token"

  val testSensor = Sensor(MacAddress("FE:26:88:7A:66:66"))
  val testTimestamp = Timestamp(OffsetDateTime.parse("2025-01-01T12:00:00Z"))
  val testMeasurement = Measurement(
    testSensor,
    MeasurementType.Temperature,
    testTimestamp,
    Value(22.5)
  )

  def spec = suite("MeasurementsProgram")(
    test("getMeasurements returns measurements for valid JWT") {
      for
        authService <- ZIO.succeed(NoopAuth(testUser))
        measurementsService <- InMemoryMeasurementsService.make
        program = MeasurementsProgram(authService, measurementsService)

        // Add a measurement first
        _ <- measurementsService.addMeasurements(
          testUser,
          NonEmptyList(testMeasurement)
        )

        // Get measurements
        result <- program.getMeasurements(
          validJwt,
          MacAddress("FE:26:88:7A:66:66"),
          MeasurementType.Temperature,
          OffsetDateTime.parse("2025-01-01T00:00:00Z"),
          OffsetDateTime.parse("2025-01-01T23:59:59Z")
        )
      yield assertTrue(
        result.size == 1,
        result.head == testMeasurement
      )
    },
    test("getMeasurements returns empty list for invalid JWT") {
      val authService = new Auth:
        def findUser(token: String): Task[Option[User]] =
          ZIO.succeed(None)

      for
        measurementsService <- InMemoryMeasurementsService.make
        program = MeasurementsProgram(authService, measurementsService)

        // Get measurements with invalid token
        result <- program.getMeasurements(
          invalidJwt,
          MacAddress("FE:26:88:7A:66:66"),
          MeasurementType.Temperature,
          OffsetDateTime.parse("2025-01-01T00:00:00Z"),
          OffsetDateTime.parse("2025-01-01T23:59:59Z")
        )
      yield assertTrue(result.isEmpty)
    },
    test("getMeasurements filters by MAC address") {
      for
        authService <- ZIO.succeed(NoopAuth(testUser))
        measurementsService <- InMemoryMeasurementsService.make
        program = MeasurementsProgram(authService, measurementsService)

        // Add measurements for different sensors
        sensor1 = Sensor(MacAddress("FE:26:88:7A:66:66"))
        sensor2 = Sensor(MacAddress("D5:12:34:66:14:14"))
        measurement1 = testMeasurement.copy(sensor = sensor1)
        measurement2 = testMeasurement.copy(sensor = sensor2)

        _ <- measurementsService.addMeasurements(
          testUser,
          NonEmptyList(measurement1, measurement2)
        )

        // Get measurements for sensor-1 only
        result <- program.getMeasurements(
          validJwt,
          MacAddress("FE:26:88:7A:66:66"),
          MeasurementType.Temperature,
          OffsetDateTime.parse("2025-01-01T00:00:00Z"),
          OffsetDateTime.parse("2025-01-01T23:59:59Z")
        )
      yield assertTrue(
        result.size == 1,
        result.head.sensor == sensor1
      )
    },
    test("getMeasurements filters by measurement type") {
      for
        authService <- ZIO.succeed(NoopAuth(testUser))
        measurementsService <- InMemoryMeasurementsService.make
        program = MeasurementsProgram(authService, measurementsService)

        // Add measurements of different types
        tempMeasurement =
          testMeasurement.copy(measurementType = MeasurementType.Temperature)
        humidityMeasurement =
          testMeasurement.copy(measurementType = MeasurementType.Humidity)

        _ <- measurementsService.addMeasurements(
          testUser,
          NonEmptyList(tempMeasurement, humidityMeasurement)
        )

        // Get temperature measurements only
        result <- program.getMeasurements(
          validJwt,
          MacAddress("FE:26:88:7A:66:66"),
          MeasurementType.Temperature,
          OffsetDateTime.parse("2025-01-01T00:00:00Z"),
          OffsetDateTime.parse("2025-01-01T23:59:59Z")
        )
      yield assertTrue(
        result.size == 1,
        result.head.measurementType == MeasurementType.Temperature
      )
    },
    test("addMeasurements stores measurements for valid JWT") {
      for
        authService <- ZIO.succeed(NoopAuth(testUser))
        measurementsService <- InMemoryMeasurementsService.make
        program = MeasurementsProgram(authService, measurementsService)

        // Add measurements
        _ <- program.addMeasurements(
          validJwt,
          NonEmptyList(testMeasurement)
        )

        // Verify measurements were stored
        result <- measurementsService.getMeasurements(
          testUser,
          MacAddress("FE:26:88:7A:66:66"),
          MeasurementType.Temperature,
          OffsetDateTime.parse("2025-01-01T00:00:00Z"),
          OffsetDateTime.parse("2025-01-01T23:59:59Z")
        )
      yield assertTrue(
        result.size == 1,
        result.head == testMeasurement
      )
    },
    test("addMeasurements fails for invalid JWT") {
      val authService = new Auth:
        def findUser(token: String): Task[Option[User]] =
          ZIO.succeed(None)

      for
        measurementsService <- InMemoryMeasurementsService.make
        program = MeasurementsProgram(authService, measurementsService)

        // Try to add measurements with invalid token
        result <- program
          .addMeasurements(invalidJwt, NonEmptyList(testMeasurement))
          .either
      yield assertTrue(result.isLeft)
    },
    test("addMeasurements handles multiple measurements") {
      for
        authService <- ZIO.succeed(NoopAuth(testUser))
        measurementsService <- InMemoryMeasurementsService.make
        program = MeasurementsProgram(authService, measurementsService)

        // Create multiple measurements
        measurement1 = testMeasurement.copy(
          timestamp = Timestamp(OffsetDateTime.parse("2025-01-01T12:00:00Z"))
        )
        measurement2 = testMeasurement.copy(
          timestamp = Timestamp(OffsetDateTime.parse("2025-01-01T13:00:00Z"))
        )
        measurement3 = testMeasurement.copy(
          timestamp = Timestamp(OffsetDateTime.parse("2025-01-01T14:00:00Z"))
        )

        // Add multiple measurements
        _ <- program.addMeasurements(
          validJwt,
          NonEmptyList(measurement1, measurement2, measurement3)
        )

        // Verify all measurements were stored
        result <- program.getMeasurements(
          validJwt,
          MacAddress("FE:26:88:7A:66:66"),
          MeasurementType.Temperature,
          OffsetDateTime.parse("2025-01-01T00:00:00Z"),
          OffsetDateTime.parse("2025-01-01T23:59:59Z")
        )
      yield assertTrue(result.size == 3)
    }
  )
