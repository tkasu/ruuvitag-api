package com.github.tkasu.ruuvitag.api.http.dto

import zio.json.*
import com.github.tkasu.ruuvitag.api.domain.measurement.Measurement

// DTO matching the OpenAPI spec for POST /telemetry/{sensorName}
case class TelemetryDataDto(
    telemetry_type: String,
    data: List[Measurement]
)

object TelemetryDataDto:
  given JsonDecoder[TelemetryDataDto] = DeriveJsonDecoder.gen[TelemetryDataDto]
  given JsonEncoder[TelemetryDataDto] = DeriveJsonEncoder.gen[TelemetryDataDto]
