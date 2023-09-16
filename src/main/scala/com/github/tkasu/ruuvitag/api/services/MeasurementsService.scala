package com.github.tkasu.ruuvitag.api.services

import java.time.OffsetDateTime
import zio.Task
import zio.prelude.NonEmptyList
import com.github.tkasu.ruuvitag.api.domain.measurement.*
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType
import com.github.tkasu.ruuvitag.api.domain.sensor.SensorName
import com.github.tkasu.ruuvitag.api.domain.user.User

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
