package com.github.tkasu.ruuvitag.api.domain

import zio.json.*

object sensor:
  case class SensorName(value: String)

  object SensorName:
    given JsonEncoder[SensorName] = JsonEncoder[String].contramap(_.value)
    given JsonDecoder[SensorName] = JsonDecoder[String].map(SensorName(_))

  case class Sensor(name: SensorName)

  object Sensor:
    given JsonEncoder[Sensor] = DeriveJsonEncoder.gen[Sensor]
    given JsonDecoder[Sensor] = DeriveJsonDecoder.gen[Sensor]
