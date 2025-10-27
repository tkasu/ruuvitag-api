package com.github.tkasu.ruuvitag.api

import zio.*
import zio.http.*
import zio.logging.backend.SLF4J
import zio.metrics.connectors.{prometheus, MetricsConfig}
import zio.metrics.connectors.prometheus.PrometheusPublisher
import com.github.tkasu.ruuvitag.api.config.AppConfig
import com.github.tkasu.ruuvitag.api.services.*
import com.github.tkasu.ruuvitag.api.programs.MeasurementsProgram
import com.github.tkasu.ruuvitag.api.http.routes.Routes
import com.github.tkasu.ruuvitag.api.domain.user.{User, UserId, UserName}
import java.util.UUID
import javax.sql.DataSource
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

object Main extends ZIOAppDefault:

  // Create DataSource for SQLite with proper resource management
  private def createDataSource(
      dbPath: String
  ): ZIO[Scope, Throwable, DataSource] =
    ZIO.acquireRelease(
      ZIO.attempt {
        val hikariConfig = new HikariConfig()
        hikariConfig.setJdbcUrl(s"jdbc:sqlite:$dbPath")
        hikariConfig.setDriverClassName("org.sqlite.JDBC")
        hikariConfig.setMaximumPoolSize(10)
        hikariConfig.setConnectionTimeout(30000)
        new HikariDataSource(hikariConfig)
      }
    )(ds =>
      ZIO
        .attempt(ds.asInstanceOf[HikariDataSource].close())
        .orDie
    )

  // Initialize services based on configuration
  private def initializeServices(
      config: AppConfig
  ): ZIO[Scope, Throwable, (Auth, MeasurementsService, HealthCheck)] =
    for
      // Initialize Auth service
      authService <- config.auth.mode match
        case "noop" =>
          ZIO.succeed(
            NoopAuth(
              User(
                UserId(UUID.fromString("00000000-0000-0000-0000-000000000000")),
                UserName("default-user")
              )
            )
          )
        case other =>
          ZIO.fail(
            new IllegalArgumentException(s"Unknown auth mode: $other")
          )

      // Initialize MeasurementsService
      measurementsService <- config.storage.mode match
        case "in-memory" => InMemoryMeasurementsService.make
        case "sqlite" =>
          for
            dataSource <- createDataSource(config.storage.database.path)
            _ <- SqliteMeasurementsService.initSchema(dataSource)
            service = SqliteMeasurementsService(
              io.getquill.jdbczio.Quill.Sqlite(
                io.getquill.SnakeCase,
                dataSource
              )
            )
          yield service
        case other =>
          ZIO.fail(
            new IllegalArgumentException(s"Unknown storage mode: $other")
          )

      // Initialize HealthCheck
      healthCheck = NoopHealthCheck
    yield (authService, measurementsService, healthCheck)

  // Create the HTTP application
  private def createApp(
      config: AppConfig
  ): ZIO[Scope, Throwable, Routes[PrometheusPublisher, Response]] =
    for
      _ <- ZIO.logInfo(s"Initializing services...")
      (authService, measurementsService, healthCheck) <- initializeServices(
        config
      )
      _ <- ZIO.logInfo(
        s"Services initialized (auth=${config.auth.mode}, storage=${config.storage.mode})"
      )

      // Create programs
      measurementsProgram = MeasurementsProgram(
        authService,
        measurementsService
      )

      // Create routes
      routes = Routes.make(healthCheck, measurementsProgram)
      _ <- ZIO.logInfo("HTTP routes configured")
    yield routes

  // Main application
  def run: ZIO[Any, Throwable, Unit] =
    val program = ZIO.scoped {
      for
        _ <- ZIO.logInfo("Starting ruuvitag-api...")

        // Load configuration
        config <- ZIO.service[AppConfig]
        _ <- ZIO.logInfo(
          s"Configuration loaded: ${config.server.host}:${config.server.port}"
        )

        // Create HTTP application
        app <- createApp(config)

        // Start server
        _ <- ZIO.logInfo(
          s"Starting HTTP server on ${config.server.host}:${config.server.port}"
        )
        _ <- Server
          .serve(app)
          .provide(
            Server.defaultWith(serverConfig =>
              serverConfig
                .binding(config.server.host, config.server.port)
            ),
            ZLayer.succeed(MetricsConfig(1.second)),
            prometheus.prometheusLayer,
            prometheus.publisherLayer
          )
      yield ()
    }

    program.provide(
      AppConfig.layer,
      Runtime.removeDefaultLoggers >>> SLF4J.slf4j
    )
