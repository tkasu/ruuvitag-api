package com.github.tkasu.ruuvitag.api.http.routes

import zio.http.{Routes as ZioRoutes, Response, Middleware}
import com.github.tkasu.ruuvitag.api.programs.MeasurementsProgram
import com.github.tkasu.ruuvitag.api.services.HealthCheck
import zio.metrics.connectors.prometheus.PrometheusPublisher

object Routes:

  def make(
      healthCheck: HealthCheck,
      measurementsProgram: MeasurementsProgram
  ): ZioRoutes[PrometheusPublisher, Response] =
    val allRoutes =
      HealthRoutes.routes(healthCheck) ++ MeasurementRoutes.routes(
        measurementsProgram
      ) ++ MetricsRoutes.routes

    // Apply built-in metrics middleware to track HTTP requests
    allRoutes @@ Middleware.metrics()
