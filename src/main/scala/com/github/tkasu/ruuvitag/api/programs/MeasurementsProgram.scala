package com.github.tkasu.ruuvitag.api.programs

import java.time.OffsetDateTime
import zio.*
import com.github.tkasu.ruuvitag.api.domain.measurement.Measurement
import com.github.tkasu.ruuvitag.api.domain.measurementtype.*
import com.github.tkasu.ruuvitag.api.domain.sensor.*
import com.github.tkasu.ruuvitag.api.services.{Auth, MeasurementsService}

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
