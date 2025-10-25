package com.github.tkasu.ruuvitag.api.http.routes

import zio.http.{Routes as ZioRoutes, Response}
import com.github.tkasu.ruuvitag.api.programs.MeasurementsProgram
import com.github.tkasu.ruuvitag.api.services.HealthCheck
import zio.metrics.connectors.prometheus.PrometheusPublisher

object Routes:
  def make(
      healthCheck: HealthCheck,
      measurementsProgram: MeasurementsProgram
  ): ZioRoutes[PrometheusPublisher, Response] =
    HealthRoutes.routes(healthCheck) ++ MeasurementRoutes.routes(
      measurementsProgram
    ) ++ MetricsRoutes.routes
