package com.github.tkasu.ruuvitag.api.services

import java.time.OffsetDateTime

import cats.data.NonEmptyList

import com.github.tkasu.ruuvitag.api.domain.measurement._
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType
import com.github.tkasu.ruuvitag.api.domain.sensor.SensorName
import com.github.tkasu.ruuvitag.api.domain.user.User

trait MeasurementsService[F[_]] {
  def getMeasurements(
      user: User,
      sensorName: SensorName,
      measurementType: MeasurementType,
      from: OffsetDateTime,
      to: OffsetDateTime
  ): F[List[Measurement]]

  def addMeasurements(
      user: User,
      measurements: NonEmptyList[Measurement]
  ): F[Unit]
}
