package com.github.tkasu.ruuvitag.api.services

import java.time.OffsetDateTime
import java.util.UUID
import zio.*
import zio.test.*
import zio.prelude.NonEmptyList
import com.github.tkasu.ruuvitag.api.domain.measurement.*
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType
import com.github.tkasu.ruuvitag.api.domain.sensor.{Sensor, SensorName}
import com.github.tkasu.ruuvitag.api.domain.user.{User, UserId, UserName}

object InMemoryMeasurementsServiceSpec extends ZIOSpecDefault:

  val testUser1 = User(UserId(UUID.randomUUID()), UserName("user1"))
  val testUser2 = User(UserId(UUID.randomUUID()), UserName("user2"))
  val sensor1 = Sensor(SensorName("sensor-1"))
  val sensor2 = Sensor(SensorName("sensor-2"))
  val baseTime = OffsetDateTime.now()

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

  def spec = suite("InMemoryMeasurementsServiceSpec")(
    test("should add and retrieve measurements for a user") {
      for
        service <- InMemoryMeasurementsService.make
        _ <- service.addMeasurements(
          testUser1,
          NonEmptyList(tempMeasurement1, tempMeasurement2)
        )
        measurements <- service.getMeasurements(
          testUser1,
          sensor1.name,
          MeasurementType.Temperature,
          baseTime.minusHours(1),
          baseTime.plusHours(2)
        )
      yield assertTrue(
        measurements.length == 2,
        measurements.contains(tempMeasurement1),
        measurements.contains(tempMeasurement2)
      )
    },
    test("should filter measurements by sensor name") {
      for
        service <- InMemoryMeasurementsService.make
        _ <- service.addMeasurements(
          testUser1,
          NonEmptyList(tempMeasurement1, sensor2Measurement)
        )
        sensor1Measurements <- service.getMeasurements(
          testUser1,
          sensor1.name,
          MeasurementType.Temperature,
          baseTime.minusHours(1),
          baseTime.plusHours(2)
        )
        sensor2Measurements <- service.getMeasurements(
          testUser1,
          sensor2.name,
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
    },
    test("should filter measurements by measurement type") {
      for
        service <- InMemoryMeasurementsService.make
        _ <- service.addMeasurements(
          testUser1,
          NonEmptyList(tempMeasurement1, humidityMeasurement)
        )
        tempMeasurements <- service.getMeasurements(
          testUser1,
          sensor1.name,
          MeasurementType.Temperature,
          baseTime.minusHours(1),
          baseTime.plusHours(3)
        )
        humidityMeasurements <- service.getMeasurements(
          testUser1,
          sensor1.name,
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
    },
    test("should filter measurements by time range") {
      for
        service <- InMemoryMeasurementsService.make
        _ <- service.addMeasurements(
          testUser1,
          NonEmptyList(tempMeasurement1, tempMeasurement2, humidityMeasurement)
        )
        // Query only the first hour
        earlyMeasurements <- service.getMeasurements(
          testUser1,
          sensor1.name,
          MeasurementType.Temperature,
          baseTime.minusHours(1),
          baseTime.plusMinutes(30)
        )
        // Query from 1 hour onwards
        laterMeasurements <- service.getMeasurements(
          testUser1,
          sensor1.name,
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
    },
    test("should isolate measurements by user") {
      for
        service <- InMemoryMeasurementsService.make
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
          sensor1.name,
          MeasurementType.Temperature,
          baseTime.minusHours(1),
          baseTime.plusHours(2)
        )
        user2Measurements <- service.getMeasurements(
          testUser2,
          sensor1.name,
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
    },
    test("should return empty list when no measurements match filters") {
      for
        service <- InMemoryMeasurementsService.make
        _ <- service.addMeasurements(
          testUser1,
          NonEmptyList(tempMeasurement1)
        )
        measurements <- service.getMeasurements(
          testUser1,
          sensor1.name,
          MeasurementType.Pressure, // Different type
          baseTime.minusHours(1),
          baseTime.plusHours(2)
        )
      yield assertTrue(measurements.isEmpty)
    },
    test("should handle multiple batches of measurements") {
      for
        service <- InMemoryMeasurementsService.make
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
          sensor1.name,
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
  )
