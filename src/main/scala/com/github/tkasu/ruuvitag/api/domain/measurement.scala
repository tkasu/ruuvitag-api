package com.github.tkasu.ruuvitag.api.domain

import java.time.OffsetDateTime

import com.github.tkasu.ruuvitag.api.domain.measurementtype.*
import com.github.tkasu.ruuvitag.api.domain.sensor.*

object measurement:
  case class Timestamp(value: OffsetDateTime)
  case class Value(value: Double)
  case class Measurement(
      sensor: Sensor,
      measurementType: MeasurementType,
      timestamp: Timestamp,
      value: Value
  )
