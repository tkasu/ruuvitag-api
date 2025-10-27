package com.github.tkasu.ruuvitag.api.http.dto

import zio.json.*

/** DTO matching the OpenAPI spec for POST /telemetry
  *
  * Request body structure: [ { "telemetry_type": "temperature", "data": [ {
  * "mac_address": "FE:26:88:7A:66:66", "timestamp": 1640995200000, "value":
  * 22.5 } ] } ]
  *
  * Note: timestamp is in milliseconds since Unix epoch
  */
case class TelemetryDataDto(
    telemetry_type: String,
    data: List[MeasurementDto]
)

object TelemetryDataDto:
  given JsonDecoder[TelemetryDataDto] = DeriveJsonDecoder.gen[TelemetryDataDto]
  given JsonEncoder[TelemetryDataDto] = DeriveJsonEncoder.gen[TelemetryDataDto]
