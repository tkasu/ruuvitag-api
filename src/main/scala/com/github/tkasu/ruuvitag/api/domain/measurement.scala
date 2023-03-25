package com.github.tkasu.ruuvitag.api.domain

import java.time.OffsetDateTime

import io.estatico.newtype.macros.newtype

import com.github.tkasu.ruuvitag.api.domain.measurementtype._
import com.github.tkasu.ruuvitag.api.domain.sensor._

object measurement {
  @newtype case class Timestamp(value: OffsetDateTime)
  @newtype case class Value(value: Double)
  case class Measurement(sensor: Sensor, measurementType: MeasurementType, timestamp: Timestamp, value: Value)
}
