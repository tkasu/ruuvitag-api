package com.github.tkasu.ruuvitag.api.http.dto

import zio.json.*
import com.github.tkasu.ruuvitag.api.domain.measurement.{
  Measurement,
  Timestamp,
  Value
}
import com.github.tkasu.ruuvitag.api.domain.sensor.{Sensor, SensorName}
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType

/** DTO matching the OpenAPI spec Measurement schema
  *
  * OpenAPI spec defines Measurement with: - sensor_name: string - timestamp:
  * integer (unix ms) - value: number
  *
  * Note: measurementType is NOT included in this DTO as it's specified in the
  * URL path for GET requests
  */
case class MeasurementDto(
    sensor_name: String,
    timestamp: Long, // unix timestamp in milliseconds
    value: Double
)

object MeasurementDto:
  given JsonEncoder[MeasurementDto] = DeriveJsonEncoder.gen[MeasurementDto]
  given JsonDecoder[MeasurementDto] = DeriveJsonDecoder.gen[MeasurementDto]

  /** Convert domain Measurement to DTO */
  def fromDomain(measurement: Measurement): MeasurementDto =
    MeasurementDto(
      sensor_name = measurement.sensor.name.value,
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
      sensor = Sensor(SensorName(dto.sensor_name)),
      measurementType = measurementType,
      timestamp = Timestamp(
        java.time.OffsetDateTime.ofInstant(
          java.time.Instant.ofEpochMilli(dto.timestamp),
          java.time.ZoneOffset.UTC
        )
      ),
      value = Value(dto.value)
    )
