package com.github.tkasu.ruuvitag.api.domain

import io.estatico.newtype.macros.newtype

object sensor {
  @newtype case class SensorName(value: String)
  case class Sensor(name: SensorName)
}
