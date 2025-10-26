package com.github.tkasu.ruuvitag.api.http.routes

import zio.*
import zio.http.{Routes as ZioRoutes, Response}
import zio.metrics.{Metric, MetricLabel}
import com.github.tkasu.ruuvitag.api.programs.MeasurementsProgram
import com.github.tkasu.ruuvitag.api.services.HealthCheck
import zio.metrics.connectors.prometheus.PrometheusPublisher

object Routes:

  // HTTP metrics - simple counters to demonstrate metrics are working
  val httpRequestsTotal = Metric.counter("http_requests_total")
  val httpHealthCheckRequests =
    Metric.counter("http_health_check_requests_total")
  val httpMetricsRequests = Metric.counter("http_metrics_requests_total")
  val httpTelemetryRequests = Metric.counter("http_telemetry_requests_total")

  def make(
      healthCheck: HealthCheck,
      measurementsProgram: MeasurementsProgram
  ): ZioRoutes[PrometheusPublisher, Response] =
    HealthRoutes.routes(healthCheck) ++ MeasurementRoutes.routes(
      measurementsProgram
    ) ++ MetricsRoutes.routes
