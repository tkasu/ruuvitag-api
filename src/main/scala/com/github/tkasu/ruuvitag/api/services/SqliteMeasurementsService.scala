package com.github.tkasu.ruuvitag.api.services

import io.getquill.*
import io.getquill.jdbczio.Quill
import java.time.OffsetDateTime
import javax.sql.DataSource
import zio.{Task, ZIO, ZLayer}
import zio.prelude.NonEmptyList
import com.github.tkasu.ruuvitag.api.domain.measurement.*
import com.github.tkasu.ruuvitag.api.domain.measurementtype.MeasurementType
import com.github.tkasu.ruuvitag.api.domain.sensor.{MacAddress, Sensor}
import com.github.tkasu.ruuvitag.api.domain.user.User

/** SQLite implementation of MeasurementsService using Quill.
  *
  * This implementation provides persistent storage of measurements using SQLite
  * database. Data persists between application restarts.
  *
  * Thread-safe: Yes, JDBC connection pooling handles concurrency. Time range
  * filtering: Inclusive boundaries (from <= timestamp <= to).
  *
  * @param quill
  *   Quill context for type-safe SQL queries
  */
final case class SqliteMeasurementsService(quill: Quill.Sqlite[SnakeCase])
    extends MeasurementsService:

  import quill.*

  // Database row representation
  case class MeasurementRow(
      id: Option[Long],
      userId: String,
      macAddress: String,
      measurementType: String,
      timestamp: Long,
      value: Double,
      createdAt: Option[Long]
  )

  // Custom table naming to match schema
  inline given schema: SchemaMeta[MeasurementRow] =
    schemaMeta[MeasurementRow]("measurements")

  def getMeasurements(
      user: User,
      macAddress: MacAddress,
      measurementType: MeasurementType,
      from: OffsetDateTime,
      to: OffsetDateTime
  ): Task[List[Measurement]] =
    val fromMillis = from.toInstant.toEpochMilli
    val toMillis = to.toInstant.toEpochMilli
    val userIdStr = user.id.value.toString
    val macStr = macAddress.value
    val typeStr = measurementType.toString

    val q = quote {
      query[MeasurementRow]
        .filter(r =>
          r.userId == lift(userIdStr) &&
            r.macAddress == lift(macStr) &&
            r.measurementType == lift(typeStr) &&
            r.timestamp >= lift(fromMillis) &&
            r.timestamp <= lift(toMillis)
        )
        .sortBy(_.timestamp)(Ord.desc)
    }

    run(q).map { rows =>
      rows.map(rowToMeasurement)
    }

  def addMeasurements(
      user: User,
      measurements: NonEmptyList[Measurement]
  ): Task[Unit] =
    val userIdStr = user.id.value.toString
    val rows = measurements.toList.map { m =>
      MeasurementRow(
        id = None,
        userId = userIdStr,
        macAddress = m.sensor.macAddress.value,
        measurementType = m.measurementType.toString,
        timestamp = m.timestamp.value.toInstant.toEpochMilli,
        value = m.value.value,
        createdAt = None
      )
    }

    val insertQ = quote {
      liftQuery(rows).foreach { row =>
        query[MeasurementRow]
          .insertValue(row)
      }
    }

    run(insertQ).unit

  private def rowToMeasurement(row: MeasurementRow): Measurement =
    Measurement(
      sensor = Sensor(MacAddress(row.macAddress)),
      measurementType = MeasurementType.valueOf(row.measurementType),
      timestamp = Timestamp(
        OffsetDateTime.ofInstant(
          java.time.Instant.ofEpochMilli(row.timestamp),
          java.time.ZoneOffset.UTC
        )
      ),
      value = Value(row.value)
    )

object SqliteMeasurementsService:

  /** Creates a ZLayer that provides SqliteMeasurementsService.
    *
    * Requires a DataSource to be available in the environment.
    */
  val layer: ZLayer[DataSource, Nothing, SqliteMeasurementsService] =
    ZLayer.fromFunction { (dataSource: DataSource) =>
      val quill = Quill.Sqlite(SnakeCase, dataSource)
      SqliteMeasurementsService(quill)
    }

  /** Initializes the database schema.
    *
    * Creates tables and indexes if they don't exist. This is safe to call on
    * every application startup.
    *
    * @param dataSource
    *   The database connection
    */
  def initSchema(dataSource: DataSource): Task[Unit] =
    ZIO.attempt {
      val schema = scala.io.Source
        .fromResource("schema.sql")
        .mkString
      val connection = dataSource.getConnection()
      try {
        val statement = connection.createStatement()
        try {
          // SQLite supports executing multiple statements
          statement.executeUpdate(schema)
        } finally {
          statement.close()
        }
      } finally {
        connection.close()
      }
    }
