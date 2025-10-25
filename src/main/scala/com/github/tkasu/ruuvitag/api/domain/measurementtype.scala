package com.github.tkasu.ruuvitag.api.domain

import zio.json.*

object measurementtype:
  enum MeasurementType:
    case Temperature, Pressure, Humidity

  object MeasurementType:
    given JsonEncoder[MeasurementType] =
      JsonEncoder[String].contramap(_.toString)
    given JsonDecoder[MeasurementType] = JsonDecoder[String].mapOrFail {
      case "Temperature" => Right(MeasurementType.Temperature)
      case "Pressure"    => Right(MeasurementType.Pressure)
      case "Humidity"    => Right(MeasurementType.Humidity)
      case other         => Left(s"Invalid measurement type: $other")
    }
