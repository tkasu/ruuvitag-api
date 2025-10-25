package com.github.tkasu.ruuvitag.api.services

import java.time.OffsetDateTime
import zio.{Task, UIO, Ref, ZIO}
import zio.prelude.NonEmptyList
import com.github.tkasu.ruuvitag.api.domain.measurement.*
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType
import com.github.tkasu.ruuvitag.api.domain.sensor.SensorName
import com.github.tkasu.ruuvitag.api.domain.user.{User, UserId}

final case class InMemoryMeasurementsService(
    storage: Ref[List[(UserId, Measurement)]]
) extends MeasurementsService:

  def getMeasurements(
      user: User,
      sensorName: SensorName,
      measurementType: MeasurementType,
      from: OffsetDateTime,
      to: OffsetDateTime
  ): Task[List[Measurement]] =
    storage.get.map { allMeasurements =>
      allMeasurements
        .filter { case (userId, measurement) =>
          userId == user.id &&
          measurement.sensor.name == sensorName &&
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
  def make: UIO[InMemoryMeasurementsService] =
    Ref
      .make(List.empty[(UserId, Measurement)])
      .map(InMemoryMeasurementsService(_))
