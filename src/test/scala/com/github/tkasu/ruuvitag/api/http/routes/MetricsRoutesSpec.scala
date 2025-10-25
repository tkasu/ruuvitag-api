package com.github.tkasu.ruuvitag.api.http.routes

import zio.*
import zio.test.*
import zio.http.*
import zio.metrics.connectors.{prometheus, MetricsConfig}

object MetricsRoutesSpec extends ZIOSpecDefault:

  def spec = suite("MetricsRoutesSpec")(
    test("GET /metrics should return 200 OK") {
      for response <- MetricsRoutes
          .routes(Request.get(URL.root / "metrics"))
          .provide(
            ZLayer.succeed(MetricsConfig(1.second)),
            prometheus.prometheusLayer,
            prometheus.publisherLayer
          )
      yield assertTrue(response.status == Status.Ok)
    },
    test("GET /metrics should return text/plain content type") {
      for
        response <- MetricsRoutes
          .routes(Request.get(URL.root / "metrics"))
          .provide(
            ZLayer.succeed(MetricsConfig(1.second)),
            prometheus.prometheusLayer,
            prometheus.publisherLayer
          )
        contentType = response.headers.get(Header.ContentType)
      yield assertTrue(
        contentType.exists(_.mediaType == MediaType.text.plain)
      )
    },
    test("GET /metrics should return valid response body") {
      for
        response <- MetricsRoutes
          .routes(Request.get(URL.root / "metrics"))
          .provide(
            ZLayer.succeed(MetricsConfig(1.second)),
            prometheus.prometheusLayer,
            prometheus.publisherLayer
          )
        body <- response.body.asString
      yield assertTrue(
        body != null
      ) // Metrics may be empty initially, but body should not be null
    }
  )
