package com.github.tkasu.ruuvitag.api.http.routes

import zio.*
import zio.test.*
import zio.http.*
import zio.metrics.connectors.{prometheus, MetricsConfig}
import zio.metrics.connectors.prometheus.PrometheusPublisher

object MetricsRoutesSpec extends ZIOSpecDefault:

  private def provideLayers[R, E, A](
      effect: ZIO[PrometheusPublisher, E, A]
  ): ZIO[Any, E, A] =
    effect.provide(
      ZLayer.succeed(MetricsConfig(1.second)),
      prometheus.prometheusLayer,
      prometheus.publisherLayer
    )

  def spec = suite("MetricsRoutesSpec")(
    test("GET /metrics should return 200 OK") {
      for response <- provideLayers(
          MetricsRoutes.routes(Request.get(URL.root / "metrics"))
        )
      yield assertTrue(response.status == Status.Ok)
    },
    test("GET /metrics should return text/plain content type") {
      for
        response <- provideLayers(
          MetricsRoutes.routes(Request.get(URL.root / "metrics"))
        )
        contentType = response.headers.get(Header.ContentType)
      yield assertTrue(
        contentType.exists(_.mediaType == MediaType.text.plain)
      )
    },
    test("GET /metrics should successfully retrieve response body") {
      for
        response <- provideLayers(
          MetricsRoutes.routes(Request.get(URL.root / "metrics"))
        )
        _ <- response.body.asString
      yield assertCompletes
    }
  )
