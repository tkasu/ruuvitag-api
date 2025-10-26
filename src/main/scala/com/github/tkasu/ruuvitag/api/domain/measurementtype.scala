package com.github.tkasu.ruuvitag.api.domain

import zio.json.*

object measurementtype:
  enum MeasurementType:
    case Temperature, Pressure, Humidity, Battery, TxPower, MovementCounter,
      MeasurementSequenceNumber

  object MeasurementType:
    given JsonEncoder[MeasurementType] =
      JsonEncoder[String].contramap(_.toString)
    given JsonDecoder[MeasurementType] = JsonDecoder[String].mapOrFail {
      case "Temperature"     => Right(MeasurementType.Temperature)
      case "Pressure"        => Right(MeasurementType.Pressure)
      case "Humidity"        => Right(MeasurementType.Humidity)
      case "Battery"         => Right(MeasurementType.Battery)
      case "TxPower"         => Right(MeasurementType.TxPower)
      case "MovementCounter" => Right(MeasurementType.MovementCounter)
      case "MeasurementSequenceNumber" =>
        Right(MeasurementType.MeasurementSequenceNumber)
      case other => Left(s"Invalid measurement type: $other")
    }
