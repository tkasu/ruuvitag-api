package com.github.tkasu.ruuvitag.api.http.dto

import zio.json.*
import com.github.tkasu.ruuvitag.api.domain.measurement.{
  Measurement,
  Timestamp,
  Value
}
import com.github.tkasu.ruuvitag.api.domain.sensor.{Sensor, MacAddress}
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType

/** DTO matching the OpenAPI spec Measurement schema
  *
  * OpenAPI spec defines Measurement with: - mac_address: string (format:
  * XX:XX:XX:XX:XX:XX) - timestamp: integer (unix ms) - value: number
  *
  * Note: measurementType is NOT included in this DTO as it's specified in the
  * URL path or request body for GET/POST requests
  */
case class MeasurementDto(
    mac_address: String,
    timestamp: Long, // unix timestamp in milliseconds
    value: Double
)

object MeasurementDto:
  given JsonEncoder[MeasurementDto] = DeriveJsonEncoder.gen[MeasurementDto]
  given JsonDecoder[MeasurementDto] = DeriveJsonDecoder.gen[MeasurementDto]

  /** Convert domain Measurement to DTO */
  def fromDomain(measurement: Measurement): MeasurementDto =
    MeasurementDto(
      mac_address = measurement.sensor.macAddress.value,
      timestamp = measurement.timestamp.value.toInstant.toEpochMilli,
      value = measurement.value.value
    )

  /** Convert DTO to domain Measurement
    *
    * @param measurementType
    *   The measurement type (from URL path or request)
    */
  def toDomain(
      dto: MeasurementDto,
      measurementType: MeasurementType
  ): Measurement =
    Measurement(
      sensor = Sensor(MacAddress(dto.mac_address)),
      measurementType = measurementType,
      timestamp = Timestamp(
        java.time.OffsetDateTime.ofInstant(
          java.time.Instant.ofEpochMilli(dto.timestamp),
          java.time.ZoneOffset.UTC
        )
      ),
      value = Value(dto.value)
    )
