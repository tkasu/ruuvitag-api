package com.github.tkasu.ruuvitag.api.programs

import java.time.OffsetDateTime
import zio.*
import zio.prelude.NonEmptyList
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
      macAddress: MacAddress,
      measurementType: MeasurementType,
      from: OffsetDateTime,
      to: OffsetDateTime
  ): Task[List[Measurement]] = for
    maybeUser <- auth.findUser(userJwt)
    measurements <- maybeUser
      .map(user =>
        measurementsService
          .getMeasurements(user, macAddress, measurementType, from, to)
      )
      .getOrElse(ZIO.succeed(List.empty))
  yield measurements

  def addMeasurements(
      userJwt: String,
      measurements: NonEmptyList[Measurement]
  ): Task[Unit] = for
    maybeUser <- auth.findUser(userJwt)
    _ <- maybeUser match
      case Some(user) =>
        measurementsService.addMeasurements(user, measurements)
      case None =>
        ZIO.fail(
          new IllegalArgumentException("Invalid authentication token")
        )
  yield ()
