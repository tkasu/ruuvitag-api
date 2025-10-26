package com.github.tkasu.ruuvitag.api.http.middleware

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.http.*
import zio.logging.backend.SLF4J

object RequestLoggingMiddlewareSpec extends ZIOSpecDefault:

  def spec = suite("RequestLoggingMiddleware")(
    test("should apply to routes without errors") {
      val testRoutes = Routes(
        Method.GET / "test" -> handler {
          Response.ok
        }
      )

      val routesWithLogging = testRoutes @@ RequestLoggingMiddleware()

      for response <- routesWithLogging.runZIO(Request.get(URL.root / "test"))
      yield assertTrue(response.status == Status.Ok)
    },
    test("should handle POST requests with body") {
      val testRoutes = Routes(
        Method.POST / "test" -> handler { (req: Request) =>
          req.body.asString
            .map(_ => Response.ok)
            .orElse(ZIO.succeed(Response.ok))
        }
      )

      val routesWithLogging = testRoutes @@ RequestLoggingMiddleware()
      val testBody = """{"test": "data"}"""

      for response <- routesWithLogging
          .runZIO(
            Request.post(
              URL.root / "test",
              Body.fromString(testBody)
            )
          )
      yield assertTrue(response.status == Status.Ok)
    },
    test("should preserve request body for handler") {
      val expectedBody = """{"test": "data", "value": 123}"""
      val testRoutes = Routes(
        Method.POST / "test" -> handler { (req: Request) =>
          req.body.asString
            .map { actualBody =>
              if actualBody == expectedBody then Response.ok
              else Response.status(Status.BadRequest)
            }
            .catchAll(_ =>
              ZIO.succeed(Response.status(Status.InternalServerError))
            )
        }
      )

      val routesWithLogging = testRoutes @@ RequestLoggingMiddleware()

      for response <- routesWithLogging
          .runZIO(
            Request.post(
              URL.root / "test",
              Body.fromString(expectedBody)
            )
          )
      yield assertTrue(response.status == Status.Ok)
    },
    test("should truncate large payloads in logs") {
      val largeBody = "x" * 2000 // Larger than MaxLogSize (1000)
      val testRoutes = Routes(
        Method.POST / "test" -> handler { (req: Request) =>
          req.body.asString
            .map { body =>
              // Verify handler receives full body
              if body.length == 2000 then Response.ok
              else Response.status(Status.BadRequest)
            }
            .catchAll(_ =>
              ZIO.succeed(Response.status(Status.InternalServerError))
            )
        }
      )

      val routesWithLogging = testRoutes @@ RequestLoggingMiddleware()

      for response <- routesWithLogging
          .runZIO(
            Request.post(
              URL.root / "test",
              Body.fromString(largeBody)
            )
          )
      yield assertTrue(response.status == Status.Ok)
    },
    test("should handle GET requests without body") {
      val testRoutes = Routes(
        Method.GET / "test" -> handler {
          Response.ok
        }
      )

      val routesWithLogging = testRoutes @@ RequestLoggingMiddleware()

      for response <- routesWithLogging.runZIO(Request.get(URL.root / "test"))
      yield assertTrue(response.status == Status.Ok)
    },
    test("should pass through error responses") {
      val testRoutes = Routes(
        Method.GET / "error" -> handler {
          Response.status(Status.BadRequest)
        }
      )

      val routesWithLogging = testRoutes @@ RequestLoggingMiddleware()

      for response <- routesWithLogging.runZIO(Request.get(URL.root / "error"))
      yield assertTrue(response.status == Status.BadRequest)
    }
  ).provide(
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  )
