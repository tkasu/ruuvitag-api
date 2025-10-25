package com.github.tkasu.ruuvitag.api.http.dto

import zio.json.*

/** DTO matching the OpenAPI spec for POST /telemetry/{sensorName}
  *
  * Request body structure: [ { "telemetry_type": "temperature", "data": [ {
  * "sensor_name": "...", "timestamp": 1234567890, "value": 22.5 } ] } ]
  */
case class TelemetryDataDto(
    telemetry_type: String,
    data: List[MeasurementDto]
)

object TelemetryDataDto:
  given JsonDecoder[TelemetryDataDto] = DeriveJsonDecoder.gen[TelemetryDataDto]
  given JsonEncoder[TelemetryDataDto] = DeriveJsonEncoder.gen[TelemetryDataDto]
