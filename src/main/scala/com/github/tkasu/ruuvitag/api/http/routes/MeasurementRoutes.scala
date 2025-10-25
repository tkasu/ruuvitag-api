package com.github.tkasu.ruuvitag.api.http.routes

import java.time.{OffsetDateTime, Instant, ZoneOffset}
import zio.*
import zio.http.*
import zio.json.*
import zio.prelude.NonEmptyList
import com.github.tkasu.ruuvitag.api.programs.MeasurementsProgram
import com.github.tkasu.ruuvitag.api.domain.sensor.SensorName
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType
import com.github.tkasu.ruuvitag.api.http.dto.{
  TelemetryDataDto,
  ErrorResponseDto
}

object MeasurementRoutes:

  // Helper to extract JWT from Authorization header
  private def extractJwt(request: Request): String =
    request.header(Header.Authorization)
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
    str match
      case "Temperature" | "temperature" =>
        Right(MeasurementType.Temperature)
      case "Pressure" | "pressure" => Right(MeasurementType.Pressure)
      case "Humidity" | "humidity" => Right(MeasurementType.Humidity)
      case _ => Left(s"Invalid measurement type: $str")

  def routes(program: MeasurementsProgram): Routes[Any, Response] =
    Routes(
      // GET /telemetry/{telemetryType}/{sensorName}?from=<timestamp>&to=<timestamp>
      Method.GET / "telemetry" / string("telemetryType") / string(
        "sensorName"
      ) -> handler { (telemetryType: String, sensorName: String, req: Request) =>
        val result = for
          // Parse query parameters
          fromStr <- ZIO
            .fromOption(req.url.queryParams.queryParam("from").headOption)
            .orElseFail("Missing 'from' query parameter")
          toStr <- ZIO
            .fromOption(req.url.queryParams.queryParam("to").headOption)
            .orElseFail("Missing 'to' query parameter")

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
            SensorName(sensorName),
            measurementType,
            from,
            to
          )
        yield Response.json(measurements.toJson)

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
      // POST /telemetry/{sensorName}
      Method.POST / "telemetry" / string("sensorName") -> handler {
        (sensorName: String, req: Request) =>
          val result = for
            // Parse request body
            bodyStr <- req.body.asString
            telemetryDataList <- ZIO
              .fromEither(bodyStr.fromJson[List[TelemetryDataDto]])
              .mapError(err => s"Invalid JSON: $err")

            // Collect all measurements from all telemetry data objects
            allMeasurements = telemetryDataList.flatMap(_.data)

            // Convert to NonEmptyList
            measurements <- ZIO
              .fromOption(NonEmptyList.fromIterableOption(allMeasurements))
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
