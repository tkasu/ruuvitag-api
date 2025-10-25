package com.github.tkasu.ruuvitag.api.http.routes

import zio.*
import zio.http.*
import zio.metrics.connectors.prometheus.PrometheusPublisher

object MetricsRoutes:
  def routes: Routes[PrometheusPublisher, Response] =
    Routes(
      Method.GET / "metrics" -> handler {
        for
          publisher <- ZIO.service[PrometheusPublisher]
          metrics <- publisher.get
          response = Response.text(metrics)
        yield response
      }
    )
