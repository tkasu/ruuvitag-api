package com.github.tkasu.ruuvitag.api.services

import java.time.OffsetDateTime
import zio.{Task, UIO, Ref, ZIO}
import zio.prelude.NonEmptyList
import com.github.tkasu.ruuvitag.api.domain.measurement.*
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType
import com.github.tkasu.ruuvitag.api.domain.sensor.MacAddress
import com.github.tkasu.ruuvitag.api.domain.user.{User, UserId}

/** In-memory implementation of MeasurementsService for testing and development.
  *
  * This implementation stores measurements in application memory using ZIO Ref
  * for thread-safe concurrent access. Measurements are not persisted between
  * application restarts.
  *
  * Performance: O(n) filtering on getMeasurements where n is total
  * measurements. Thread-safe: Yes, via ZIO Ref atomic operations. Time range
  * filtering: Inclusive boundaries (from <= timestamp <= to).
  *
  * Intended use cases:
  *   - Development and testing without database setup
  *   - Mock service for integration testing
  *   - Demonstration and prototype environments
  *
  * @param storage
  *   Thread-safe reference to in-memory measurement storage
  */
final case class InMemoryMeasurementsService(
    storage: Ref[List[(UserId, Measurement)]]
) extends MeasurementsService:

  def getMeasurements(
      user: User,
      macAddress: MacAddress,
      measurementType: MeasurementType,
      from: OffsetDateTime,
      to: OffsetDateTime
  ): Task[List[Measurement]] =
    storage.get.map { allMeasurements =>
      allMeasurements
        .filter { case (userId, measurement) =>
          userId == user.id &&
          measurement.sensor.macAddress == macAddress &&
          measurement.measurementType == measurementType &&
          !measurement.timestamp.value.isBefore(from) &&
          !measurement.timestamp.value.isAfter(to)
        }
        .map(_._2)
    }

  def addMeasurements(
      user: User,
      measurements: NonEmptyList[Measurement]
  ): Task[Unit] =
    storage.update { currentMeasurements =>
      val newMeasurements = measurements.toList.map(m => (user.id, m))
      currentMeasurements ++ newMeasurements
    }

object InMemoryMeasurementsService:
  /** Creates a new InMemoryMeasurementsService with empty storage.
    *
    * @return
    *   ZIO effect that cannot fail, producing a new service instance
    */
  def make: UIO[InMemoryMeasurementsService] =
    Ref
      .make(List.empty[(UserId, Measurement)])
      .map(InMemoryMeasurementsService(_))
