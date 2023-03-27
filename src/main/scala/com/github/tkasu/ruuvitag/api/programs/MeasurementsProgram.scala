package com.github.tkasu.ruuvitag.api.programs

import java.time.OffsetDateTime

import cats.Monad
import cats.syntax.all._
import dev.profunktor.auth.jwt.JwtToken

import com.github.tkasu.ruuvitag.api.domain.measurement.Measurement
import com.github.tkasu.ruuvitag.api.domain.measurementtype._
import com.github.tkasu.ruuvitag.api.domain.sensor._
import com.github.tkasu.ruuvitag.api.services.{ Auth, MeasurementsService }

final case class MeasurementsProgram[F[_]: Monad](
    auth: Auth[F],
    measurementsService: MeasurementsService[F]
) {

  def getMeasurements(
      userJwt: JwtToken,
      sensorName: SensorName,
      measurementType: MeasurementType,
      from: OffsetDateTime,
      to: OffsetDateTime
  ): F[List[Measurement]] = for {
    maybeUser <- auth.findUser(userJwt)
    measurements <- maybeUser
      .map(user => measurementsService.getMeasurements(user, sensorName, measurementType, from, to))
      .getOrElse(Monad[F].pure(List.empty))
  } yield measurements

}
