package com.github.tkasu.ruuvitag.api.domain

import java.time.OffsetDateTime
import zio.json.*

import com.github.tkasu.ruuvitag.api.domain.measurementtype.*
import com.github.tkasu.ruuvitag.api.domain.sensor.*

object measurement:
  case class Timestamp(value: OffsetDateTime)

  object Timestamp:
    // Encode as unix timestamp in milliseconds
    given JsonEncoder[Timestamp] =
      JsonEncoder[Long].contramap(ts => ts.value.toInstant.toEpochMilli)
    // Decode from unix timestamp in milliseconds
    given JsonDecoder[Timestamp] = JsonDecoder[Long].map { millis =>
      Timestamp(
        OffsetDateTime.ofInstant(
          java.time.Instant.ofEpochMilli(millis),
          java.time.ZoneOffset.UTC
        )
      )
    }

  case class Value(value: Double)

  object Value:
    given JsonEncoder[Value] = JsonEncoder[Double].contramap(_.value)
    given JsonDecoder[Value] = JsonDecoder[Double].map(Value(_))

  case class Measurement(
      sensor: Sensor,
      measurementType: MeasurementType,
      timestamp: Timestamp,
      value: Value
  )

  object Measurement:
    given JsonEncoder[Measurement] = DeriveJsonEncoder.gen[Measurement]
    given JsonDecoder[Measurement] = DeriveJsonDecoder.gen[Measurement]
