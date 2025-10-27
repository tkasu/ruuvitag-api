package com.github.tkasu.ruuvitag.api.http.routes

import java.time.{OffsetDateTime, Instant, ZoneOffset}
import zio.*
import zio.http.*
import zio.json.*
import zio.prelude.NonEmptyList
import com.github.tkasu.ruuvitag.api.programs.MeasurementsProgram
import com.github.tkasu.ruuvitag.api.domain.sensor.MacAddress
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType
import com.github.tkasu.ruuvitag.api.http.dto.{
  TelemetryDataDto,
  MeasurementDto,
  ErrorResponseDto
}

object MeasurementRoutes:

  // Helper to extract JWT from Authorization header
  private def extractJwt(request: Request): String =
    request
      .header(Header.Authorization)
      .map(_.renderedValue)
      .flatMap { authHeader =>
        if authHeader.startsWith("Bearer ")
        then Some(authHeader.substring(7))
        else None
      }
      .getOrElse("no-token") // Default token for NoopAuth

  // Helper to parse ISO 8601 timestamp or unix millis
  private def parseTimestamp(str: String): Either[String, OffsetDateTime] =
    try
      // Try parsing as ISO 8601 first
      Right(OffsetDateTime.parse(str))
    catch
      case _: Exception =>
        // Try parsing as unix milliseconds
        try
          val millis = str.toLong
          Right(
            OffsetDateTime.ofInstant(
              Instant.ofEpochMilli(millis),
              ZoneOffset.UTC
            )
          )
        catch
          case _: Exception =>
            Left(s"Invalid timestamp format: $str")

  // Helper to parse MeasurementType from string
  private def parseMeasurementType(
      str: String
  ): Either[String, MeasurementType] =
    str.toLowerCase match
      case "temperature"      => Right(MeasurementType.Temperature)
      case "pressure"         => Right(MeasurementType.Pressure)
      case "humidity"         => Right(MeasurementType.Humidity)
      case "battery"          => Right(MeasurementType.Battery)
      case "tx_power"         => Right(MeasurementType.TxPower)
      case "movement_counter" => Right(MeasurementType.MovementCounter)
      case "measurement_sequence_number" =>
        Right(MeasurementType.MeasurementSequenceNumber)
      case _ => Left(s"Invalid measurement type: $str")

  // Helper to parse and validate MAC address
  private def parseMacAddress(str: String): Either[String, MacAddress] =
    try Right(MacAddress(str))
    catch
      case e: IllegalArgumentException =>
        Left(s"Invalid MAC address: ${e.getMessage}")

  def routes(program: MeasurementsProgram): Routes[Any, Response] =
    Routes(
      // GET /telemetry/{telemetryType}?macAddress=XX:XX:XX:XX:XX:XX&from=<timestamp>&to=<timestamp>
      Method.GET / "telemetry" / string("telemetryType") -> handler {
        (telemetryType: String, req: Request) =>
          val result =
            for
              // Parse query parameters
              macAddressStr <- ZIO
                .fromOption(
                  req.url.queryParams.queryParam("macAddress").headOption
                )
                .orElseFail("Missing 'macAddress' query parameter")
              fromStr <- ZIO
                .fromOption(req.url.queryParams.queryParam("from").headOption)
                .orElseFail("Missing 'from' query parameter")
              toStr <- ZIO
                .fromOption(req.url.queryParams.queryParam("to").headOption)
                .orElseFail("Missing 'to' query parameter")

              // Parse and validate MAC address
              macAddress <- ZIO.fromEither(parseMacAddress(macAddressStr))

              // Parse timestamps
              from <- ZIO.fromEither(parseTimestamp(fromStr))
              to <- ZIO.fromEither(parseTimestamp(toStr))

              // Parse measurement type
              measurementType <- ZIO.fromEither(
                parseMeasurementType(telemetryType)
              )

              // Extract JWT and get measurements
              jwt = extractJwt(req)
              measurements <- program.getMeasurements(
                jwt,
                macAddress,
                measurementType,
                from,
                to
              )

              // Convert domain models to DTOs matching OpenAPI spec
              measurementDtos = measurements.map(MeasurementDto.fromDomain)
            yield Response.json(measurementDtos.toJson)

          result.catchAll { error =>
            ZIO.succeed(
              Response
                .json(
                  ErrorResponseDto("BadRequest", error.toString).toJson
                )
                .status(Status.BadRequest)
            )
          }
      },
      // POST /telemetry
      Method.POST / "telemetry" -> handler { (req: Request) =>
        val result =
          for
            // Parse request body
            bodyStr <- req.body.asString
            telemetryDataList <- ZIO
              .fromEither(bodyStr.fromJson[List[TelemetryDataDto]])
              .mapError(err => s"Invalid JSON: $err")

            // Convert DTOs to domain models
            // For each TelemetryDataDto, parse the telemetry_type and convert measurements
            allMeasurements <- ZIO.foreach(telemetryDataList) { telemetryData =>
              for
                measurementType <- ZIO.fromEither(
                  parseMeasurementType(telemetryData.telemetry_type)
                )
                // Convert each MeasurementDto to domain Measurement
                // This will validate MAC addresses in the DTOs
                measurements <- ZIO.foreach(telemetryData.data) { dto =>
                  ZIO
                    .attempt(MeasurementDto.toDomain(dto, measurementType))
                    .mapError(e =>
                      s"Invalid measurement data for MAC address '${dto.mac_address}': ${e.getMessage}"
                    )
                }
              yield measurements
            }

            // Flatten to single list and convert to NonEmptyList
            measurements <- ZIO
              .fromOption(
                NonEmptyList.fromIterableOption(allMeasurements.flatten)
              )
              .orElseFail("No measurements provided")

            // Extract JWT and add measurements
            jwt = extractJwt(req)
            _ <- program.addMeasurements(jwt, measurements)
          yield Response.status(Status.Created)

        result.catchAll { error =>
          ZIO.succeed(
            Response
              .json(
                ErrorResponseDto("BadRequest", error.toString).toJson
              )
              .status(Status.BadRequest)
          )
        }
      }
    )
